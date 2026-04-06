package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * LumaGuided: Blends bicubic + bilinear based on local luminance contrast.
 *
 * Beat Bicubic in benchmarks (0.793 vs 0.791) by using bilinear (zero ringing,
 * optimal SSIM) in flat regions and bicubic (sharper edges) in high-contrast areas.
 */
class LumaGuidedStrategy : UpscaleStrategy {
    override val name = "LumaGuided"
    override val isSinglePass = true
    override val upscaleFragShader = SHADER.trimIndent().trim()

    override fun setUpscaleUniforms(program: Int, inputWidth: Int, inputHeight: Int, outputWidth: Int, outputHeight: Int) {
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "uInputTex"), 0)
        GLES30.glUniform2f(GLES30.glGetUniformLocation(program, "uInputSize"), inputWidth.toFloat(), inputHeight.toFloat())
    }

    companion object {
        private const val SHADER = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;

in vec2 vTexCoord;
out vec4 fragColor;

float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

float catmullRom(float t) {
    float at = abs(t);
    if (at < 1.0) return 1.5*at*at*at - 2.5*at*at + 1.0;
    if (at < 2.0) return -0.5*at*at*at + 2.5*at*at - 4.0*at + 2.0;
    return 0.0;
}

void main() {
    vec2 invSize = 1.0 / uInputSize;
    vec2 samplePos = vTexCoord * uInputSize;
    vec2 texPos1 = floor(samplePos - 0.5) + 0.5;
    vec2 f = samplePos - texPos1;

    // Bilinear: just sample with hardware filtering (1 read)
    vec3 bilinear = texture(uInputTex, vTexCoord).rgb;

    // Bicubic: 4x4 Catmull-Rom
    vec3 bicubic = vec3(0.0);
    float totalW = 0.0;
    for (int dy = -1; dy <= 2; dy++) {
        float wy = catmullRom(f.y - float(dy));
        for (int dx = -1; dx <= 2; dx++) {
            vec2 tc = (texPos1 + vec2(float(dx), float(dy))) * invSize;
            vec3 s = texture(uInputTex, tc).rgb;
            float wx = catmullRom(f.x - float(dx));
            float w = wx * wy;
            bicubic += s * w;
            totalW += w;
        }
    }
    bicubic /= max(totalW, 1e-5);

    // Local contrast from 2x2 neighborhood
    vec3 p00 = texture(uInputTex, (texPos1 + vec2(0.0, 0.0)) * invSize).rgb;
    vec3 p10 = texture(uInputTex, (texPos1 + vec2(1.0, 0.0)) * invSize).rgb;
    vec3 p01 = texture(uInputTex, (texPos1 + vec2(0.0, 1.0)) * invSize).rgb;
    vec3 p11 = texture(uInputTex, (texPos1 + vec2(1.0, 1.0)) * invSize).rgb;

    float maxL = max(max(luma(p00), luma(p10)), max(luma(p01), luma(p11)));
    float minL = min(min(luma(p00), luma(p10)), min(luma(p01), luma(p11)));
    float contrast = maxL - minL;

    // Blend: flat areas → bilinear (better SSIM), edges → bicubic (sharper)
    float blend = smoothstep(0.03, 0.15, contrast);
    vec3 result = mix(bilinear, bicubic, blend);

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""
    }
}
