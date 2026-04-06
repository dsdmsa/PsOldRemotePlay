package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Custom experimental upscaler for PS2 streaming.
 *
 * Approach: Edge-preserving bicubic with luminance-guided adaptive sharpening.
 *
 * Pass 1 (Upscale): Modified Catmull-Rom that detects edge direction via
 * luminance gradients and biases the bicubic kernel toward the edge direction.
 * Flat areas get smooth bicubic, edges get directional interpolation.
 *
 * Pass 2 (Sharpen): Luminance-weighted unsharp mask that only sharpens
 * mid-luminance regions (avoids sharpening dark noise and bright highlights).
 *
 * This is designed for compressed PS2 video where:
 * - Aggressive edge enhancement (FSR) amplifies H.264 block artifacts
 * - Pure bicubic is too soft on character edges and text
 * - We want edge awareness WITHOUT the ringing artifacts of Lanczos
 */
class CustomUpscaleStrategy : UpscaleStrategy {
    override val name = "Custom (Experimental)"

    override val upscaleFragShader = UPSCALE_FRAG.trimIndent().trim()
    override val sharpenFragShader = SHARPEN_FRAG.trimIndent().trim()

    override fun setUpscaleUniforms(program: Int, inputWidth: Int, inputHeight: Int, outputWidth: Int, outputHeight: Int) {
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "uInputTex"), 0)
        GLES30.glUniform2f(GLES30.glGetUniformLocation(program, "uInputSize"), inputWidth.toFloat(), inputHeight.toFloat())
        GLES30.glUniform2f(GLES30.glGetUniformLocation(program, "uOutputSize"), outputWidth.toFloat(), outputHeight.toFloat())
    }

    override fun setSharpenUniforms(program: Int, outputWidth: Int, outputHeight: Int, sharpness: Float) {
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "uInputTex"), 0)
        GLES30.glUniform2f(GLES30.glGetUniformLocation(program, "uTexelSize"), 1f / outputWidth, 1f / outputHeight)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, "uSharpness"), sharpness)
    }

    companion object {
        /**
         * Edge-Preserving Bicubic Upscaler
         *
         * Uses standard Catmull-Rom as base, but detects local edge direction
         * and blends between isotropic (smooth) and directional (sharp) interpolation.
         *
         * The key idea: at each output pixel, compute the Sobel gradient of the input.
         * If the gradient magnitude is low (flat area), use standard bicubic.
         * If high (edge), bias the interpolation weights along the edge direction
         * to avoid blurring across the edge.
         */
        private const val UPSCALE_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;
uniform vec2 uOutputSize;

in vec2 vTexCoord;
out vec4 fragColor;

float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

// Catmull-Rom weight for distance t from center
float catmullRom(float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    float at = abs(t);
    if (at < 1.0) return 1.5 * t3 - 2.5 * t2 + 1.0;
    if (at < 2.0) return -0.5 * t3 + 2.5 * t2 - 4.0 * at + 2.0;
    return 0.0;
}

void main() {
    vec2 ps = 1.0 / uInputSize;
    vec2 srcPos = vTexCoord * uInputSize;
    vec2 center = floor(srcPos - 0.5) + 0.5;
    vec2 f = srcPos - center;
    vec2 tc = center * ps;

    // Sample 4x4 neighborhood
    vec3 samples[16];
    float lumas[16];
    for (int y = -1; y <= 2; y++) {
        for (int x = -1; x <= 2; x++) {
            int idx = (y + 1) * 4 + (x + 1);
            vec2 offset = vec2(float(x), float(y)) * ps;
            samples[idx] = texture(uInputTex, tc + offset).rgb;
            lumas[idx] = luma(samples[idx]);
        }
    }

    // Sobel edge detection on 3x3 center region
    // Horizontal: [-1 0 1; -2 0 2; -1 0 1]
    // Vertical:   [-1 -2 -1; 0 0 0; 1 2 1]
    // Using indices: row0=(0,1,2,3), row1=(4,5,6,7), row2=(8,9,10,11), row3=(12,13,14,15)
    // Center 3x3 is rows 0-2, cols 0-2 = indices 5,6,7, 9,10,11, but shifted
    float gx = -lumas[0] + lumas[2] - 2.0*lumas[4] + 2.0*lumas[6] - lumas[8] + lumas[10];
    float gy = -lumas[0] - 2.0*lumas[1] - lumas[2] + lumas[8] + 2.0*lumas[9] + lumas[10];
    float edgeMag = sqrt(gx * gx + gy * gy);

    // Adaptive blend: 0 = pure bicubic (flat), 1 = edge-aware (strong edge)
    float edgeBlend = smoothstep(0.02, 0.15, edgeMag);

    // Standard Catmull-Rom bicubic (isotropic)
    vec3 bicubicResult = vec3(0.0);
    float totalWeight = 0.0;
    for (int y = -1; y <= 2; y++) {
        float wy = catmullRom(f.y - float(y));
        for (int x = -1; x <= 2; x++) {
            float wx = catmullRom(f.x - float(x));
            float w = wx * wy;
            int idx = (y + 1) * 4 + (x + 1);
            bicubicResult += samples[idx] * w;
            totalWeight += w;
        }
    }
    bicubicResult /= max(totalWeight, 1e-5);

    // Edge-aware: weight samples more that are similar to the nearest neighbor
    // This preserves edges by avoiding mixing across them
    vec3 nearest = samples[5 + (f.y > 0.5 ? 4 : 0) + (f.x > 0.5 ? 1 : 0)];
    float nearestLuma = luma(nearest);

    vec3 edgeResult = vec3(0.0);
    float edgeWeight = 0.0;
    for (int y = -1; y <= 2; y++) {
        float wy = catmullRom(f.y - float(y));
        for (int x = -1; x <= 2; x++) {
            float wx = catmullRom(f.x - float(x));
            int idx = (y + 1) * 4 + (x + 1);
            // Bilateral-like weight: reduce weight for pixels with different luminance
            float lumaDiff = abs(lumas[idx] - nearestLuma);
            float bilateral = exp(-lumaDiff * lumaDiff * 50.0);
            float w = wx * wy * bilateral;
            edgeResult += samples[idx] * w;
            edgeWeight += w;
        }
    }
    edgeResult /= max(edgeWeight, 1e-5);

    // Blend between smooth bicubic and edge-preserving
    vec3 result = mix(bicubicResult, edgeResult, edgeBlend);

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""

        /**
         * Luminance-Weighted Unsharp Mask
         *
         * Only sharpens mid-luminance regions. Dark areas (noise) and
         * bright highlights (clipping risk) are left alone.
         * This naturally suppresses compression artifact amplification.
         */
        private const val SHARPEN_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uTexelSize;
uniform float uSharpness;

in vec2 vTexCoord;
out vec4 fragColor;

float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

void main() {
    vec3 center = texture(uInputTex, vTexCoord).rgb;
    vec3 n = texture(uInputTex, vTexCoord + vec2(0.0, -uTexelSize.y)).rgb;
    vec3 s = texture(uInputTex, vTexCoord + vec2(0.0,  uTexelSize.y)).rgb;
    vec3 w = texture(uInputTex, vTexCoord + vec2(-uTexelSize.x, 0.0)).rgb;
    vec3 e = texture(uInputTex, vTexCoord + vec2( uTexelSize.x, 0.0)).rgb;

    // Unsharp mask: center - blur
    vec3 blur = 0.25 * (n + s + w + e);
    vec3 detail = center - blur;

    // Luminance-based masking: only sharpen mid-luminance
    float l = luma(center);
    float mask = smoothstep(0.05, 0.2, l) * smoothstep(0.95, 0.8, l);

    // Apply sharpening scaled by mask and user control
    float strength = uSharpness * 1.5 * mask;
    vec3 result = center + detail * strength;

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""
    }
}
