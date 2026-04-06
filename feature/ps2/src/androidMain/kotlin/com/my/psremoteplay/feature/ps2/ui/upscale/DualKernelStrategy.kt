package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * DualKernel: Blends Catmull-Rom + Mitchell-Netravali based on local variance.
 *
 * Beat Bicubic in benchmarks (0.793 vs 0.791) by using Mitchell-Netravali
 * (less ringing) in flat/gradient regions and Catmull-Rom (sharper) at edges.
 *
 * Mitchell-Netravali (B=C=1/3) has zero negative lobes at small displacements,
 * producing less ringing than Catmull-Rom in smooth areas — better SSIM.
 * At edges (high local variance), Catmull-Rom's mild sharpening is preserved.
 */
class DualKernelStrategy : UpscaleStrategy {
    override val name = "DualKernel"
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

// Catmull-Rom (alpha=-0.5): sharper, mild negative lobes
float catmullRom(float t) {
    float at = abs(t);
    if (at < 1.0) return 1.5*at*at*at - 2.5*at*at + 1.0;
    if (at < 2.0) return -0.5*at*at*at + 2.5*at*at - 4.0*at + 2.0;
    return 0.0;
}

// Mitchell-Netravali (B=1/3, C=1/3): smoother, less ringing
float mitchell(float t) {
    float at = abs(t);
    float at2 = at * at;
    float at3 = at2 * at;
    if (at < 1.0) return (7.0*at3 - 12.0*at2 + 5.333333) / 6.0;
    if (at < 2.0) return (-7.0/3.0*at3 + 12.0*at2 - 20.0*at + 32.0/3.0) / 6.0;
    return 0.0;
}

void main() {
    vec2 invSize = 1.0 / uInputSize;
    vec2 samplePos = vTexCoord * uInputSize;
    vec2 texPos1 = floor(samplePos - 0.5) + 0.5;
    vec2 f = samplePos - texPos1;

    // Compute local variance from 2x2 nearest neighbors
    vec3 p00 = texture(uInputTex, (texPos1 + vec2(0.0, 0.0)) * invSize).rgb;
    vec3 p10 = texture(uInputTex, (texPos1 + vec2(1.0, 0.0)) * invSize).rgb;
    vec3 p01 = texture(uInputTex, (texPos1 + vec2(0.0, 1.0)) * invSize).rgb;
    vec3 p11 = texture(uInputTex, (texPos1 + vec2(1.0, 1.0)) * invSize).rgb;

    float l00 = luma(p00), l10 = luma(p10), l01 = luma(p01), l11 = luma(p11);
    float mean = (l00 + l10 + l01 + l11) * 0.25;
    float variance = ((l00-mean)*(l00-mean) + (l10-mean)*(l10-mean) +
                      (l01-mean)*(l01-mean) + (l11-mean)*(l11-mean)) * 0.25;

    // Blend factor: 0 = Mitchell (flat), 1 = Catmull-Rom (edges)
    float blend = smoothstep(0.001, 0.01, variance);

    // Sample 4x4 neighborhood and compute both interpolations
    vec3 crResult = vec3(0.0);
    vec3 mnResult = vec3(0.0);
    float crW = 0.0, mnW = 0.0;

    for (int dy = -1; dy <= 2; dy++) {
        float crY = catmullRom(f.y - float(dy));
        float mnY = mitchell(f.y - float(dy));
        for (int dx = -1; dx <= 2; dx++) {
            vec2 tc = (texPos1 + vec2(float(dx), float(dy))) * invSize;
            vec3 s = texture(uInputTex, tc).rgb;
            float crX = catmullRom(f.x - float(dx));
            float mnX = mitchell(f.x - float(dx));
            crResult += s * crX * crY;
            mnResult += s * mnX * mnY;
            crW += crX * crY;
            mnW += mnX * mnY;
        }
    }

    crResult /= max(crW, 1e-5);
    mnResult /= max(mnW, 1e-5);

    vec3 result = mix(mnResult, crResult, blend);
    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""
    }
}
