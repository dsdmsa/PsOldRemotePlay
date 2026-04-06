package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Deblock + Bicubic upscale strategy designed for H.264 compressed video.
 *
 * Key insight: most upscalers amplify compression artifacts (block boundaries,
 * DCT ringing). This strategy removes artifacts FIRST, then upscales cleanly.
 *
 * 2-pass strategy:
 *   Pass 1: Combined deblocking + Catmull-Rom bicubic upscale. At each output pixel,
 *           a 5x5 input neighborhood is sampled. Samples near H.264 block boundaries
 *           (4px and 8px) receive bilateral smoothing to suppress blocking artifacts,
 *           while samples in non-boundary regions use standard Catmull-Rom weights.
 *           Local contrast distinguishes flat areas (stronger deblocking) from textured
 *           areas (weaker deblocking).
 *   Pass 2: Gentle edge-aware CAS sharpening that suppresses enhancement near detected
 *           block boundaries and restricts sharpening to mid-luminance regions to avoid
 *           amplifying dark noise or bright clipping.
 */
class DeblockUpscaleStrategy : UpscaleStrategy {

    override val name: String = "Deblock + Bicubic"

    override val isSinglePass: Boolean = false

    override val upscaleFragShader: String
        get() = DEBLOCK_UPSCALE_FRAG.trimIndent().trim()

    override val sharpenFragShader: String
        get() = DEBLOCK_SHARPEN_FRAG.trimIndent().trim()

    override fun setUpscaleUniforms(
        program: Int,
        inputWidth: Int,
        inputHeight: Int,
        outputWidth: Int,
        outputHeight: Int
    ) {
        val texLoc = GLES30.glGetUniformLocation(program, "uInputTex")
        val inputSizeLoc = GLES30.glGetUniformLocation(program, "uInputSize")
        val outputSizeLoc = GLES30.glGetUniformLocation(program, "uOutputSize")
        GLES30.glUniform1i(texLoc, 0)
        GLES30.glUniform2f(inputSizeLoc, inputWidth.toFloat(), inputHeight.toFloat())
        GLES30.glUniform2f(outputSizeLoc, outputWidth.toFloat(), outputHeight.toFloat())
    }

    override fun setSharpenUniforms(
        program: Int,
        outputWidth: Int,
        outputHeight: Int,
        sharpness: Float
    ) {
        val texLoc = GLES30.glGetUniformLocation(program, "uInputTex")
        val texelSizeLoc = GLES30.glGetUniformLocation(program, "uTexelSize")
        val sharpnessLoc = GLES30.glGetUniformLocation(program, "uSharpness")
        GLES30.glUniform1i(texLoc, 0)
        GLES30.glUniform2f(texelSizeLoc, 1f / outputWidth, 1f / outputHeight)
        GLES30.glUniform1f(sharpnessLoc, sharpness)
    }

    companion object {
        private const val DEBLOCK_UPSCALE_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;   // e.g., 640.0, 448.0
uniform vec2 uOutputSize;  // e.g., 1920.0, 1080.0

in vec2 vTexCoord;
out vec4 fragColor;

// Luminance using BT.709 coefficients
float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// Detect proximity to H.264 block boundary (4x4 or 8x8 transform blocks)
float blockBoundary(vec2 pixelPos) {
    vec2 m4 = mod(pixelPos, 4.0);
    vec2 m8 = mod(pixelPos, 8.0);

    // Distance to nearest 4-pixel boundary (0..2 range, 0 = on boundary)
    float d4 = min(min(m4.x, 4.0 - m4.x), min(m4.y, 4.0 - m4.y));
    // Distance to nearest 8-pixel boundary
    float d8 = min(min(m8.x, 8.0 - m8.x), min(m8.y, 8.0 - m8.y));

    // Stronger signal for 8-pixel boundaries, weaker for 4-pixel
    float b8 = smoothstep(1.5, 0.0, d8);
    float b4 = smoothstep(1.0, 0.0, d4) * 0.5;

    return clamp(b8 + b4, 0.0, 1.0);
}

void main() {
    vec2 invInputSize = 1.0 / uInputSize;

    // Map output pixel to input space
    vec2 inputPos = vTexCoord * uInputSize;
    vec2 texPos1 = floor(inputPos - 0.5) + 0.5;
    vec2 f = inputPos - texPos1;

    // Catmull-Rom basis functions
    vec2 w0 = f * (-0.5 + f * (1.0 - 0.5 * f));
    vec2 w1 = 1.0 + f * f * (-2.5 + 1.5 * f);
    vec2 w2 = f * (0.5 + f * (2.0 - 1.5 * f));
    vec2 w3 = f * f * (-0.5 + 0.5 * f);

    // Compute local contrast from a 5-tap cross at the center of the kernel
    vec2 centerUV = texPos1 * invInputSize;
    vec3 cC = texture(uInputTex, centerUV).rgb;
    vec3 cN = texture(uInputTex, centerUV + vec2(0.0, -invInputSize.y)).rgb;
    vec3 cS = texture(uInputTex, centerUV + vec2(0.0,  invInputSize.y)).rgb;
    vec3 cW = texture(uInputTex, centerUV + vec2(-invInputSize.x, 0.0)).rgb;
    vec3 cE = texture(uInputTex, centerUV + vec2( invInputSize.x, 0.0)).rgb;

    float localContrast = max(
        max(abs(luma(cN) - luma(cC)), abs(luma(cS) - luma(cC))),
        max(abs(luma(cW) - luma(cC)), abs(luma(cE) - luma(cC)))
    );
    // texturedMask: 0 in flat areas (strong deblocking), 1 in textured (weak deblocking)
    float texturedMask = smoothstep(0.02, 0.12, localContrast);

    float centerLuma = luma(cC);

    // Sample a 5x5 neighborhood (using the 4x4 Catmull-Rom grid plus extra ring)
    // We use the bilinear optimization for the core 4x4, plus boundary-aware weighting
    vec4 result = vec4(0.0);
    float totalWeight = 0.0;

    for (int j = -2; j <= 2; j++) {
        for (int i = -2; i <= 2; i++) {
            vec2 samplePixel = texPos1 + vec2(float(i), float(j));
            vec2 sampleUV = samplePixel * invInputSize;
            vec3 sampleColor = texture(uInputTex, sampleUV).rgb;

            // Base weight: Catmull-Rom for inner 4x4, Gaussian falloff for outer ring
            float wx, wy;
            if (i == -2) { wx = abs(w0.x) * 0.5; }
            else if (i == -1) { wx = abs(w0.x) + abs(w1.x); }
            else if (i == 0) { wx = abs(w1.x) + abs(w2.x); }
            else if (i == 1) { wx = abs(w2.x) + abs(w3.x); }
            else { wx = abs(w3.x) * 0.5; }

            if (j == -2) { wy = abs(w0.y) * 0.5; }
            else if (j == -1) { wy = abs(w0.y) + abs(w1.y); }
            else if (j == 0) { wy = abs(w1.y) + abs(w2.y); }
            else if (j == 1) { wy = abs(w2.y) + abs(w3.y); }
            else { wy = abs(w3.y) * 0.5; }

            float spatialWeight = wx * wy;

            // Detect if this sample crosses a block boundary relative to center
            float boundary = blockBoundary(samplePixel);

            // Bilateral weight: reduce weight if across boundary AND large luma difference
            float lumaDiff = abs(luma(sampleColor) - centerLuma);
            // Deblock strength: strong in flat areas, weak in textured areas
            float deblockStrength = mix(1.0, 0.15, texturedMask);
            float bilateralWeight = exp(-lumaDiff * lumaDiff * 50.0 * deblockStrength);

            // Blend: near boundaries use bilateral, away from boundaries use spatial only
            float weight = spatialWeight * mix(1.0, bilateralWeight, boundary * deblockStrength);

            result.rgb += sampleColor * weight;
            totalWeight += weight;
        }
    }

    result.rgb /= max(totalWeight, 0.001);
    fragColor = vec4(clamp(result.rgb, 0.0, 1.0), 1.0);
}
"""

        private const val DEBLOCK_SHARPEN_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uTexelSize;
uniform float uSharpness;  // 0.0 = no sharpen, 1.0 = max

in vec2 vTexCoord;
out vec4 fragColor;

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    // 5-tap cross neighborhood
    vec3 e = texture(uInputTex, vTexCoord).rgb;
    vec3 b = texture(uInputTex, vTexCoord + vec2(0.0, -uTexelSize.y)).rgb;
    vec3 d = texture(uInputTex, vTexCoord + vec2(-uTexelSize.x, 0.0)).rgb;
    vec3 f = texture(uInputTex, vTexCoord + vec2( uTexelSize.x, 0.0)).rgb;
    vec3 h = texture(uInputTex, vTexCoord + vec2(0.0,  uTexelSize.y)).rgb;

    // Per-channel CAS adaptive sharpening
    vec3 mn4 = min(min(b, d), min(f, h));
    vec3 mx4 = max(max(b, d), max(f, h));
    mn4 = min(mn4, e);
    mx4 = max(mx4, e);

    vec3 amp = sqrt(clamp(min(mn4, 1.0 - mx4) / mx4, 0.0, 1.0));
    amp = amp * mix(0.05, 0.1, uSharpness);

    // Block boundary suppression: detect if we are near a block boundary in output space
    // and reduce sharpening to avoid re-introducing deblocked artifacts.
    // Map output texcoord back to approximate input pixel position (assume ~2-3x upscale).
    vec2 approxInputPixel = vTexCoord / uTexelSize;
    vec2 m4 = mod(approxInputPixel * 0.5, 4.0); // approximate mapping
    vec2 m8 = mod(approxInputPixel * 0.5, 8.0);
    float d4 = min(min(m4.x, 4.0 - m4.x), min(m4.y, 4.0 - m4.y));
    float d8 = min(min(m8.x, 8.0 - m8.x), min(m8.y, 8.0 - m8.y));
    float boundaryMask = 1.0 - clamp(smoothstep(1.5, 0.0, d8) + smoothstep(1.0, 0.0, d4) * 0.5, 0.0, 1.0);

    // Luminance-weighted mask: only sharpen mid-luminance regions
    // Dark regions have noise, bright regions clip — leave both alone
    float l = luma(e);
    float lumaMask = smoothstep(0.05, 0.2, l) * smoothstep(0.95, 0.8, l);

    // Combine masks
    amp *= boundaryMask * lumaMask;

    // Apply as negative weight
    vec3 w = -amp;
    vec3 rcpW = 1.0 / (1.0 + 4.0 * w);

    fragColor = vec4(clamp((e + w * (b + d + f + h)) * rcpW, 0.0, 1.0), 1.0);
}
"""
    }
}
