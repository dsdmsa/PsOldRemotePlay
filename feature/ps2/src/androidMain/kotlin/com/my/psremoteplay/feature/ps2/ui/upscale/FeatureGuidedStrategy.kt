package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Feature-Guided Upscaler — 2-pass structure-aware upscaling.
 *
 * Pass 1 (Structure-Aware Upscale):
 *   Computes a structure tensor from 3x3 luma neighborhood to find edge direction
 *   via eigenvalue decomposition. Applies bilateral-weighted Catmull-Rom bicubic
 *   where each tap weight is scaled by luma similarity (preserves edges). Extracts
 *   and re-adds detail weighted by edge magnitude.
 *
 * Pass 2 (Directional Sharpen + Clamp):
 *   Reads edge direction from luminance gradients of the upscaled image. Applies
 *   unsharp mask perpendicular to the edge direction (sharpens edge crossings).
 *   Clamps output to local min/max to prevent halos.
 */
class FeatureGuidedStrategy : UpscaleStrategy {

    override val name: String = "Feature-Guided"

    override val isSinglePass: Boolean = false

    override val upscaleFragShader: String
        get() = UPSCALE_FRAG.trimIndent().trim()

    override val sharpenFragShader: String
        get() = SHARPEN_FRAG.trimIndent().trim()

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
        private const val UPSCALE_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;
uniform vec2 uOutputSize;

in vec2 vTexCoord;
out vec4 fragColor;

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// Catmull-Rom basis weights for parameter t in [0,1]
vec4 catmullRomWeights(float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    return vec4(
        -0.5 * t3 + t2 - 0.5 * t,
         1.5 * t3 - 2.5 * t2 + 1.0,
        -1.5 * t3 + 2.0 * t2 + 0.5 * t,
         0.5 * t3 - 0.5 * t2
    );
}

void main() {
    vec2 ps = 1.0 / uInputSize;

    // Map output pixel to input space
    vec2 srcPos = vTexCoord * uInputSize - 0.5;
    vec2 srcFloor = floor(srcPos);
    vec2 frac = srcPos - srcFloor;

    vec2 tc = (srcFloor + 0.5) * ps;
    vec2 dx = vec2(ps.x, 0.0);
    vec2 dy = vec2(0.0, ps.y);

    // ---- Gather 3x3 neighborhood for structure tensor ----
    vec3 s00 = texture(uInputTex, tc - dx - dy).rgb;
    vec3 s10 = texture(uInputTex, tc      - dy).rgb;
    vec3 s20 = texture(uInputTex, tc + dx - dy).rgb;
    vec3 s01 = texture(uInputTex, tc - dx     ).rgb;
    vec3 s11 = texture(uInputTex, tc          ).rgb;
    vec3 s21 = texture(uInputTex, tc + dx     ).rgb;
    vec3 s02 = texture(uInputTex, tc - dx + dy).rgb;
    vec3 s12 = texture(uInputTex, tc      + dy).rgb;
    vec3 s22 = texture(uInputTex, tc + dx + dy).rgb;

    float l00 = luma(s00), l10 = luma(s10), l20 = luma(s20);
    float l01 = luma(s01), l11 = luma(s11), l21 = luma(s21);
    float l02 = luma(s02), l12 = luma(s12), l22 = luma(s22);

    // ---- Structure tensor via Sobel gradients ----
    float gx = -l00 + l20 - 2.0 * l01 + 2.0 * l21 - l02 + l22;
    float gy = -l00 - 2.0 * l10 - l20 + l02 + 2.0 * l12 + l22;

    // Structure tensor components: J = sum(grad * grad^T)
    // For a single-scale estimate, use the Sobel gradient directly
    float Jxx = gx * gx;
    float Jyy = gy * gy;
    float Jxy = gx * gy;

    // ---- Eigenvalue decomposition for edge direction ----
    // Eigenvalues of 2x2 symmetric matrix [Jxx Jxy; Jxy Jyy]:
    // lambda = 0.5 * (trace +/- sqrt(trace^2 - 4*det))
    float trace = Jxx + Jyy;
    float det = Jxx * Jyy - Jxy * Jxy;
    float disc = sqrt(max(trace * trace * 0.25 - det, 0.0));
    float lambda1 = 0.5 * trace + disc;  // larger eigenvalue
    float lambda2 = 0.5 * trace - disc;  // smaller eigenvalue

    // Edge magnitude from eigenvalue ratio (coherence)
    float coherence = (lambda1 - lambda2) / (lambda1 + lambda2 + 1e-5);
    float edgeMag = sqrt(gx * gx + gy * gy);

    // Edge direction: eigenvector of larger eigenvalue
    // For [Jxx-lambda2, Jxy], normalized
    vec2 edgeDir = vec2(Jxy, lambda1 - Jxx);
    float edgeDirLen = length(edgeDir);
    edgeDir = edgeDirLen > 1e-5 ? edgeDir / edgeDirLen : vec2(1.0, 0.0);

    // ---- Bilateral-weighted Catmull-Rom bicubic (4x4) ----
    // Fetch extra samples for full 4x4 grid
    vec3 s30 = texture(uInputTex, tc + 2.0 * dx - dy).rgb;
    vec3 s31 = texture(uInputTex, tc + 2.0 * dx     ).rgb;
    vec3 s32 = texture(uInputTex, tc + 2.0 * dx + dy).rgb;
    vec3 s33 = texture(uInputTex, tc + 2.0 * dx + 2.0 * dy).rgb;
    vec3 s03 = texture(uInputTex, tc - dx + 2.0 * dy).rgb;
    vec3 s13 = texture(uInputTex, tc      + 2.0 * dy).rgb;
    vec3 s23 = texture(uInputTex, tc + dx + 2.0 * dy).rgb;

    vec4 wx = catmullRomWeights(frac.x);
    vec4 wy = catmullRomWeights(frac.y);

    // Bilateral sigma — higher coherence = stronger edge preservation
    float sigma = mix(50.0, 200.0, coherence);

    // Reference luma for bilateral weighting (bilinear estimate)
    float refLuma = mix(
        mix(l11, l21, frac.x),
        mix(l12, l22, frac.x),
        frac.y
    );

    // 4x4 grid luma values
    float l30 = luma(s30), l31 = luma(s31), l32 = luma(s32), l33 = luma(s33);
    float l03 = luma(s03), l13 = luma(s13), l23 = luma(s23);

    // Build 4x4 arrays: samples and luma
    // Row 0: s00, s10, s20, s30
    // Row 1: s01, s11, s21, s31
    // Row 2: s02, s12, s22, s32
    // Row 3: s03, s13, s23, s33

    vec3 acc = vec3(0.0);
    float wSum = 0.0;

    // Macro-like bilateral Catmull-Rom accumulation
    // For each tap: weight = catmull_weight * exp(-lumaDiff^2 * sigma)
    #define BILATERAL_TAP(sample, sampleLuma, cwx, cwy) { \
        float cw = cwx * cwy; \
        float ld = sampleLuma - refLuma; \
        float bw = exp(-ld * ld * sigma); \
        float w = abs(cw) * bw; \
        acc += sample * w * sign(cw); \
        wSum += w * sign(cw); \
    }

    // Row 0 (y weight = wy.x)
    BILATERAL_TAP(s00, l00, wx.x, wy.x)
    BILATERAL_TAP(s10, l10, wx.y, wy.x)
    BILATERAL_TAP(s20, l20, wx.z, wy.x)
    BILATERAL_TAP(s30, l30, wx.w, wy.x)

    // Row 1 (y weight = wy.y)
    BILATERAL_TAP(s01, l01, wx.x, wy.y)
    BILATERAL_TAP(s11, l11, wx.y, wy.y)
    BILATERAL_TAP(s21, l21, wx.z, wy.y)
    BILATERAL_TAP(s31, l31, wx.w, wy.y)

    // Row 2 (y weight = wy.z)
    BILATERAL_TAP(s02, l02, wx.x, wy.z)
    BILATERAL_TAP(s12, l12, wx.y, wy.z)
    BILATERAL_TAP(s22, l22, wx.z, wy.z)
    BILATERAL_TAP(s32, l32, wx.w, wy.z)

    // Row 3 (y weight = wy.w)
    BILATERAL_TAP(s03, l03, wx.x, wy.w)
    BILATERAL_TAP(s13, l13, wx.y, wy.w)
    BILATERAL_TAP(s23, l23, wx.z, wy.w)
    BILATERAL_TAP(s33, l33, wx.w, wy.w)

    #undef BILATERAL_TAP

    vec3 bicubicResult = abs(wSum) > 1e-5 ? acc / wSum : s11;

    // ---- Inline detail extraction ----
    // Gaussian blur approximation from 3x3 neighborhood (1-2-1 kernel)
    vec3 gaussBlur = (
        1.0 * s00 + 2.0 * s10 + 1.0 * s20 +
        2.0 * s01 + 4.0 * s11 + 2.0 * s21 +
        1.0 * s02 + 2.0 * s12 + 1.0 * s22
    ) / 16.0;

    vec3 detail = s11 - gaussBlur;
    // Add detail back, weighted by edge magnitude (more detail on edges)
    float detailStrength = clamp(edgeMag * 2.0, 0.0, 0.5);
    vec3 result = bicubicResult + detail * detailStrength;

    // Clamp to valid range
    result = clamp(result, 0.0, 1.0);

    fragColor = vec4(result, 1.0);
}
"""

        private const val SHARPEN_FRAG = """
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
    // ---- Read center and cross neighbors for gradient ----
    vec3 center = texture(uInputTex, vTexCoord).rgb;
    vec3 left   = texture(uInputTex, vTexCoord + vec2(-uTexelSize.x, 0.0)).rgb;
    vec3 right  = texture(uInputTex, vTexCoord + vec2( uTexelSize.x, 0.0)).rgb;
    vec3 up     = texture(uInputTex, vTexCoord + vec2(0.0, -uTexelSize.y)).rgb;
    vec3 down   = texture(uInputTex, vTexCoord + vec2(0.0,  uTexelSize.y)).rgb;

    // ---- Edge direction from luminance gradients ----
    float lCenter = luma(center);
    float lLeft   = luma(left);
    float lRight  = luma(right);
    float lUp     = luma(up);
    float lDown   = luma(down);

    float gradX = lRight - lLeft;
    float gradY = lDown - lUp;

    // Edge tangent (perpendicular to gradient = along the edge)
    vec2 edgeTangent = vec2(-gradY, gradX);
    float tangentLen = length(edgeTangent);
    edgeTangent = tangentLen > 1e-5 ? edgeTangent / tangentLen : vec2(1.0, 0.0);

    // Perpendicular to edge (direction across the edge — where we want to sharpen)
    vec2 sharpDir = vec2(edgeTangent.y, -edgeTangent.x);

    // ---- Directional unsharp mask along sharpDir ----
    vec2 offset1 = sharpDir * uTexelSize;
    vec2 offset2 = sharpDir * uTexelSize * 2.0;

    vec3 s1p = texture(uInputTex, vTexCoord + offset1).rgb;
    vec3 s1n = texture(uInputTex, vTexCoord - offset1).rgb;
    vec3 s2p = texture(uInputTex, vTexCoord + offset2).rgb;
    vec3 s2n = texture(uInputTex, vTexCoord - offset2).rgb;

    // Weighted blur along perpendicular direction (1D Gaussian: 1-4-6-4-1 / 16)
    vec3 blur = (s2n + 4.0 * s1n + 6.0 * center + 4.0 * s1p + s2p) / 16.0;

    // Unsharp mask: detail = center - blur, result = center + strength * detail
    vec3 detail = center - blur;
    float strength = uSharpness * 1.5;  // scale for perceptual range

    // Adaptive strength: reduce sharpening in flat areas to avoid noise
    float edgeMag = length(vec2(gradX, gradY));
    float adaptiveStrength = strength * smoothstep(0.01, 0.1, edgeMag);

    vec3 result = center + detail * adaptiveStrength;

    // ---- Clamp to local min/max to prevent halos ----
    vec3 localMin = min(min(left, right), min(up, down));
    vec3 localMax = max(max(left, right), max(up, down));
    localMin = min(localMin, center);
    localMax = max(localMax, center);

    // Slight expansion to avoid over-clamping (allow 10% overshoot)
    vec3 range = localMax - localMin;
    localMin -= range * 0.1;
    localMax += range * 0.1;

    result = clamp(result, localMin, localMax);
    result = clamp(result, 0.0, 1.0);

    fragColor = vec4(result, 1.0);
}
"""
    }
}
