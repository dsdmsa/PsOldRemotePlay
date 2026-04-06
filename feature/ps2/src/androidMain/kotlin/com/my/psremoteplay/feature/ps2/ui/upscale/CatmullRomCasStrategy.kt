package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Catmull-Rom bicubic upscale + Contrast Adaptive Sharpening (CAS).
 *
 * 2-pass strategy:
 *   Pass 1: Catmull-Rom bicubic interpolation using the bilinear optimization trick
 *           (9 texture samples instead of 16).
 *   Pass 2: CAS — per-channel contrast-adaptive sharpening that adapts strength
 *           based on local min/max to avoid clipping.
 *
 * Good fallback for low-end GPUs — simpler math than FSR/SGSR.
 */
class CatmullRomCasStrategy : UpscaleStrategy {

    override val name: String = "Bicubic + CAS"

    override val isSinglePass: Boolean = false

    override val upscaleFragShader: String
        get() = CATMULL_ROM_FRAG.trimIndent().trim()

    override val sharpenFragShader: String
        get() = CAS_FRAG.trimIndent().trim()

    override fun setUpscaleUniforms(
        program: Int,
        inputWidth: Int,
        inputHeight: Int,
        outputWidth: Int,
        outputHeight: Int
    ) {
        val texLoc = GLES30.glGetUniformLocation(program, "uInputTex")
        val inputSizeLoc = GLES30.glGetUniformLocation(program, "uInputSize")
        GLES30.glUniform1i(texLoc, 0)
        GLES30.glUniform2f(inputSizeLoc, inputWidth.toFloat(), inputHeight.toFloat())
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
        private const val CATMULL_ROM_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;  // e.g., 640.0, 448.0

in vec2 vTexCoord;
out vec4 fragColor;

void main() {
    vec2 texSize = uInputSize;
    vec2 invTexSize = 1.0 / texSize;

    vec2 samplePos = vTexCoord * texSize;
    vec2 texPos1 = floor(samplePos - 0.5) + 0.5;
    vec2 f = samplePos - texPos1;

    // Catmull-Rom basis functions
    vec2 w0 = f * (-0.5 + f * (1.0 - 0.5 * f));
    vec2 w1 = 1.0 + f * f * (-2.5 + 1.5 * f);
    vec2 w2 = f * (0.5 + f * (2.0 - 1.5 * f));
    vec2 w3 = f * f * (-0.5 + 0.5 * f);

    // Combine middle weights for bilinear trick
    vec2 w12 = w1 + w2;
    vec2 offset12 = w2 / w12;

    // Sampling positions
    vec2 texPos0 = (texPos1 - 1.0) * invTexSize;
    vec2 texPos3 = (texPos1 + 2.0) * invTexSize;
    vec2 texPos12 = (texPos1 + offset12) * invTexSize;

    // 9 texture samples (instead of 16)
    vec4 result = vec4(0.0);

    result += texture(uInputTex, vec2(texPos0.x,  texPos0.y))  * w0.x  * w0.y;
    result += texture(uInputTex, vec2(texPos12.x, texPos0.y))  * w12.x * w0.y;
    result += texture(uInputTex, vec2(texPos3.x,  texPos0.y))  * w3.x  * w0.y;

    result += texture(uInputTex, vec2(texPos0.x,  texPos12.y)) * w0.x  * w12.y;
    result += texture(uInputTex, vec2(texPos12.x, texPos12.y)) * w12.x * w12.y;
    result += texture(uInputTex, vec2(texPos3.x,  texPos12.y)) * w3.x  * w12.y;

    result += texture(uInputTex, vec2(texPos0.x,  texPos3.y))  * w0.x  * w3.y;
    result += texture(uInputTex, vec2(texPos12.x, texPos3.y))  * w12.x * w3.y;
    result += texture(uInputTex, vec2(texPos3.x,  texPos3.y))  * w3.x  * w3.y;

    fragColor = vec4(clamp(result.rgb, 0.0, 1.0), 1.0);
}
"""

        private const val CAS_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uTexelSize;
uniform float uSharpness;  // 0.0 = no sharpen, 1.0 = max

in vec2 vTexCoord;
out vec4 fragColor;

void main() {
    // 5-tap cross neighborhood
    vec3 e = texture(uInputTex, vTexCoord).rgb;
    vec3 b = texture(uInputTex, vTexCoord + vec2(0.0, -uTexelSize.y)).rgb;
    vec3 d = texture(uInputTex, vTexCoord + vec2(-uTexelSize.x, 0.0)).rgb;
    vec3 f = texture(uInputTex, vTexCoord + vec2( uTexelSize.x, 0.0)).rgb;
    vec3 h = texture(uInputTex, vTexCoord + vec2(0.0,  uTexelSize.y)).rgb;

    // Per-channel CAS (more accurate than luma-only)
    vec3 mn4 = min(min(b, d), min(f, h));
    vec3 mx4 = max(max(b, d), max(f, h));
    mn4 = min(mn4, e);
    mx4 = max(mx4, e);

    // Adaptive sharpening weight per channel
    vec3 amp = sqrt(clamp(min(mn4, 1.0 - mx4) / mx4, 0.0, 1.0));
    amp = amp * mix(0.075, 0.15, uSharpness);

    // Apply as negative weight
    vec3 w = -amp;
    vec3 rcpW = 1.0 / (1.0 + 4.0 * w);

    fragColor = vec4(clamp((e + w * (b + d + f + h)) * rcpW, 0.0, 1.0), 1.0);
}
"""
    }
}
