package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Matrix Filter Bank — single-pass directional upscaler.
 *
 * Gathers a 3x3 neighborhood, applies Sobel-H and Sobel-V as dot products on
 * the same 9 luma samples to detect edge direction/magnitude. Uses the result
 * to blend between Catmull-Rom bicubic (flat areas) and 1D directional cubic
 * interpolation (edges). Anti-halo clamp to local 2x2 min/max.
 */
class MatrixFilterBankStrategy : UpscaleStrategy {

    override val name: String = "Matrix Filter Bank"

    override val isSinglePass: Boolean = true

    override val upscaleFragShader: String
        get() = MFB_FRAG.trimIndent().trim()

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

    companion object {
        private const val MFB_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;
uniform vec2 uOutputSize;

in vec2 vTexCoord;
out vec4 fragColor;

// Rec. 709 luma
float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// Catmull-Rom basis: t in [0,1], returns weights for 4 taps at -1, 0, 1, 2
vec4 catmullRomWeights(float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    float w0 = -0.5 * t3 + t2 - 0.5 * t;
    float w1 = 1.5 * t3 - 2.5 * t2 + 1.0;
    float w2 = -1.5 * t3 + 2.0 * t2 + 0.5 * t;
    float w3 = 0.5 * t3 - 0.5 * t2;
    return vec4(w0, w1, w2, w3);
}

void main() {
    vec2 ps = 1.0 / uInputSize;

    // Map output pixel to input space
    vec2 srcPos = vTexCoord * uInputSize - 0.5;
    vec2 srcFloor = floor(srcPos);
    vec2 frac = srcPos - srcFloor;

    // Center of nearest input pixel
    vec2 tc = (srcFloor + 0.5) * ps;
    vec2 dx = vec2(ps.x, 0.0);
    vec2 dy = vec2(0.0, ps.y);

    // ---- Step 1: Gather 3x3 neighborhood ----
    vec3 s00 = texture(uInputTex, tc - dx - dy).rgb;
    vec3 s10 = texture(uInputTex, tc      - dy).rgb;
    vec3 s20 = texture(uInputTex, tc + dx - dy).rgb;
    vec3 s01 = texture(uInputTex, tc - dx     ).rgb;
    vec3 s11 = texture(uInputTex, tc          ).rgb;
    vec3 s21 = texture(uInputTex, tc + dx     ).rgb;
    vec3 s02 = texture(uInputTex, tc - dx + dy).rgb;
    vec3 s12 = texture(uInputTex, tc      + dy).rgb;
    vec3 s22 = texture(uInputTex, tc + dx + dy).rgb;

    // ---- Step 2: 9 luma values ----
    float l00 = luma(s00), l10 = luma(s10), l20 = luma(s20);
    float l01 = luma(s01), l11 = luma(s11), l21 = luma(s21);
    float l02 = luma(s02), l12 = luma(s12), l22 = luma(s22);

    // ---- Step 3: Sobel dot products ----
    // Sobel-H weights: -1 0 +1 / -2 0 +2 / -1 0 +1 (row-major)
    // Sobel-V weights: -1 -2 -1 / 0 0 0 / +1 +2 +1
    float gx = -1.0 * l00 + 1.0 * l20
             + -2.0 * l01 + 2.0 * l21
             + -1.0 * l02 + 1.0 * l22;

    float gy = -1.0 * l00 - 2.0 * l10 - 1.0 * l20
             +  1.0 * l02 + 2.0 * l12 + 1.0 * l22;

    // ---- Step 4: Edge magnitude and direction ----
    float edgeMag = sqrt(gx * gx + gy * gy);
    // Normalize: edge direction (tangent along the edge, perpendicular to gradient)
    vec2 edgeDir = vec2(-gy, gx);  // rotate gradient 90 degrees = edge tangent
    float edgeDirLen = length(edgeDir);
    edgeDir = edgeDirLen > 1e-5 ? edgeDir / edgeDirLen : vec2(1.0, 0.0);

    // ---- Step 5: Flat areas — Catmull-Rom bicubic (4x4 samples) ----
    // Additional samples for full 4x4 grid
    vec3 sn1n1 = s00;
    vec3 sn10  = s01;
    vec3 s0n1  = s10;
    vec3 s00c  = s11;

    // Fetch the extra ring for 4x4 Catmull-Rom
    vec3 s30 = texture(uInputTex, tc + 2.0 * dx - dy).rgb;
    vec3 s31 = texture(uInputTex, tc + 2.0 * dx     ).rgb;
    vec3 s32 = texture(uInputTex, tc + 2.0 * dx + dy).rgb;
    vec3 s33 = texture(uInputTex, tc + 2.0 * dx + 2.0 * dy).rgb;
    vec3 s03 = texture(uInputTex, tc - dx + 2.0 * dy).rgb;
    vec3 s13 = texture(uInputTex, tc      + 2.0 * dy).rgb;
    vec3 s23 = texture(uInputTex, tc + dx + 2.0 * dy).rgb;

    vec4 wx = catmullRomWeights(frac.x);
    vec4 wy = catmullRomWeights(frac.y);

    // Row-by-row Catmull-Rom
    vec3 row0 = wx.x * s00 + wx.y * s10 + wx.z * s20 + wx.w * s30;
    vec3 row1 = wx.x * s01 + wx.y * s11 + wx.z * s21 + wx.w * s31;
    vec3 row2 = wx.x * s02 + wx.y * s12 + wx.z * s22 + wx.w * s32;
    vec3 row3 = wx.x * s03 + wx.y * s13 + wx.z * s23 + wx.w * s33;

    vec3 flatResult = wy.x * row0 + wy.y * row1 + wy.z * row2 + wy.w * row3;

    // ---- Step 6: Edge areas — directional 1D cubic along edge ----
    // Project sub-pixel offset onto edge direction
    vec2 subPixel = frac - 0.5;  // centered sub-pixel offset
    float t = dot(subPixel, edgeDir);

    // Sample 4 points along edge direction at integer spacing
    vec2 edgeStep = edgeDir * ps * uInputSize;  // 1 pixel step in edge direction
    // Normalize to texcoord space
    vec2 stepTC = edgeDir * ps;

    vec2 sampleCenter = tc + frac * ps * 0.0;  // use tc as anchor
    vec3 e0 = texture(uInputTex, tc - stepTC).rgb;
    vec3 e1 = texture(uInputTex, tc).rgb;
    vec3 e2 = texture(uInputTex, tc + stepTC).rgb;
    vec3 e3 = texture(uInputTex, tc + 2.0 * stepTC).rgb;

    // 1D Catmull-Rom along edge direction
    float et = clamp(t + 0.5, 0.0, 1.0);  // remap to [0,1]
    vec4 ew = catmullRomWeights(et);
    vec3 edgeResult = ew.x * e0 + ew.y * e1 + ew.z * e2 + ew.w * e3;

    // ---- Step 7: Blend flat/edge based on edge magnitude ----
    // Threshold tuned for PS2 content (typically 480i/p upscaled)
    float edgeThresholdLow = 0.05;
    float edgeThresholdHigh = 0.25;
    float blend = smoothstep(edgeThresholdLow, edgeThresholdHigh, edgeMag);

    vec3 result = mix(flatResult, edgeResult, blend);

    // ---- Step 8: Anti-halo clamp to local 2x2 min/max ----
    vec3 localMin = min(min(s11, s21), min(s12, s22));
    vec3 localMax = max(max(s11, s21), max(s12, s22));
    result = clamp(result, localMin, localMax);

    fragColor = vec4(result, 1.0);
}
"""
    }
}
