package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Fixed 3x integer Catmull-Rom bicubic with pre-computed weights.
 *
 * Optimizations vs generic bicubic:
 * - Pre-computed const weight arrays (eliminates 36 ALU ops per pixel)
 * - Uniform 3x scale in both axes (no anisotropic distortion)
 * - Phase 0 pixels are exact copies of input (33% of pixels need no interpolation)
 * - Bilinear trick: 9 texture reads instead of 16
 *
 * The intermediate FBO should be exactly 1920x1344 (640*3 x 448*3).
 * The renderer's letterbox viewport handles fitting to the actual screen.
 */
class Fixed3xBicubicStrategy : UpscaleStrategy {
    override val name = "Fixed 3x Bicubic"
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

// Pre-computed Catmull-Rom bilinear-trick weights for exactly 3x upscaling.
// For each phase {0, 1, 2}: (w0, w12_combined, w3) and offset12 = w2/(w1+w2)
//
// Phase 0 (f=0.0): w0=0, w1=1, w2=0, w3=0 → w12=1.0, off=0.0
// Phase 1 (f=1/3): w0=-0.0741, w1=0.8889, w2=0.2222, w3=-0.0370 → w12=1.1111, off=0.2
// Phase 2 (f=2/3): w0=-0.0370, w1=0.2222, w2=0.8889, w3=-0.0741 → w12=1.1111, off=0.8

const vec3 W013[3] = vec3[3](
    vec3(0.0,       1.0,      0.0),
    vec3(-0.074074, 1.111111, -0.037037),
    vec3(-0.037037, 1.111111, -0.074074)
);
const float OFF12[3] = float[3](0.0, 0.2, 0.8);

void main() {
    vec2 invSize = 1.0 / uInputSize;
    vec2 samplePos = vTexCoord * uInputSize;
    vec2 texPos1 = floor(samplePos - 0.5) + 0.5;

    // Determine phase from sub-pixel position within the 3x grid
    vec2 f = samplePos - texPos1;
    ivec2 phase = clamp(ivec2(f * 3.0), 0, 2);

    // Lookup pre-computed weights
    vec3 wX = W013[phase.x];
    vec3 wY = W013[phase.y];
    float offX = OFF12[phase.x];
    float offY = OFF12[phase.y];

    // Sampling positions (bilinear trick combines w1+w2 into one read)
    vec2 texPos0  = (texPos1 - 1.0) * invSize;
    vec2 texPos3  = (texPos1 + 2.0) * invSize;
    vec2 texPos12 = (texPos1 + vec2(offX, offY)) * invSize;

    // 9 texture reads with pre-computed weights — zero runtime weight computation
    vec3 result =
        texture(uInputTex, vec2(texPos0.x,  texPos0.y)).rgb  * wX.x * wY.x +
        texture(uInputTex, vec2(texPos12.x, texPos0.y)).rgb  * wX.y * wY.x +
        texture(uInputTex, vec2(texPos3.x,  texPos0.y)).rgb  * wX.z * wY.x +
        texture(uInputTex, vec2(texPos0.x,  texPos12.y)).rgb * wX.x * wY.y +
        texture(uInputTex, vec2(texPos12.x, texPos12.y)).rgb * wX.y * wY.y +
        texture(uInputTex, vec2(texPos3.x,  texPos12.y)).rgb * wX.z * wY.y +
        texture(uInputTex, vec2(texPos0.x,  texPos3.y)).rgb  * wX.x * wY.z +
        texture(uInputTex, vec2(texPos12.x, texPos3.y)).rgb  * wX.y * wY.z +
        texture(uInputTex, vec2(texPos3.x,  texPos3.y)).rgb  * wX.z * wY.z;

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""
    }
}
