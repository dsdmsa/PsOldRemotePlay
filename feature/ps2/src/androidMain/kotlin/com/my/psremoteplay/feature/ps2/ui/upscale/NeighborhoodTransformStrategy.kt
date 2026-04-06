package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Algorithm E: NeighborhoodTransform — minimal matrix-multiply upscaler.
 *
 * The fastest possible "learned" upscaler architecture:
 *   1. Gather a 3x3 neighborhood (9 samples)
 *   2. Compute dominant edge direction from Sobel (4 variants)
 *   3. Select a 3x9 transform matrix from 4 pre-computed variants
 *   4. Output RGB = M x V (27 multiply-adds per output pixel)
 *
 * The 4 matrix variants are rotations of the same base filter:
 *   - Variant 0: Horizontal edge (gradient points up/down)
 *   - Variant 1: 45-degree diagonal
 *   - Variant 2: Vertical edge (gradient points left/right)
 *   - Variant 3: 135-degree diagonal
 *
 * Each variant is a 3x9 matrix where each row produces one output channel.
 * The base filter applies bicubic-like weights along the edge direction and
 * edge-preserving weights across the edge.
 *
 * Single-pass. 9 texture reads per output pixel. ~27 MADs for the multiply.
 */
class NeighborhoodTransformStrategy : UpscaleStrategy {

    override val name: String = "Neighborhood Transform"

    override val isSinglePass: Boolean = true

    override val upscaleFragShader: String
        get() = FRAG.trimIndent().trim()

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
        private const val FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;
uniform vec2 uOutputSize;

in vec2 vTexCoord;
out vec4 fragColor;

// BT.709 luma
float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// ============================================================
// 4 transform matrices (3x9 each), stored as const arrays.
//
// Base design principle: for a 3x3 neighborhood with sub-pixel offset,
// the center pixel gets dominant weight, neighbors along the detected
// edge get positive weight (interpolation), and neighbors across the
// edge get reduced weight (preservation).
//
// Matrix layout: row-major, 9 values per row, 3 rows per matrix.
// Index mapping for 3x3 neighborhood:
//   0=(-1,-1)  1=(0,-1)  2=(1,-1)
//   3=(-1, 0)  4=(0, 0)  5=(1, 0)
//   6=(-1, 1)  7=(0, 1)  8=(1, 1)
//
// For color images, all 3 rows use the same weights (the "matrix" is
// really a weight vector applied identically to R, G, B). This keeps
// the shader simple and avoids chroma artifacts.
//
// Variant 0 - Horizontal edge: interpolate along x-axis, preserve y-axis
// Variant 1 - 45-deg diagonal: interpolate along NE-SW, preserve NW-SE
// Variant 2 - Vertical edge: interpolate along y-axis, preserve x-axis
// Variant 3 - 135-deg diagonal: interpolate along NW-SE, preserve NE-SW
// ============================================================

// Sub-pixel adaptive weights for 4 edge directions.
// These are blended with the sub-pixel fractional position.

// Horizontal edge filter: strong horizontal interpolation, weak vertical
const float H_FILTER[9] = float[9](
    0.02,  0.04,  0.02,   // top row: low weight (across edge)
    0.15,  0.54,  0.15,   // center row: high weight (along edge)
    0.02,  0.04,  0.02    // bottom row: low weight (across edge)
);

// 45-degree diagonal filter
const float D45_FILTER[9] = float[9](
    0.14,  0.04,  0.02,   // top-left strong (along diagonal)
    0.04,  0.52,  0.04,   // center dominant
    0.02,  0.04,  0.14    // bottom-right strong (along diagonal)
);

// Vertical edge filter: strong vertical interpolation, weak horizontal
const float V_FILTER[9] = float[9](
    0.02,  0.15,  0.02,
    0.04,  0.54,  0.04,
    0.02,  0.15,  0.02
);

// 135-degree diagonal filter
const float D135_FILTER[9] = float[9](
    0.02,  0.04,  0.14,   // top-right strong (along diagonal)
    0.04,  0.52,  0.04,
    0.14,  0.04,  0.02    // bottom-left strong (along diagonal)
);

// Isotropic (flat area) filter: standard bilinear-like weights
const float FLAT_FILTER[9] = float[9](
    0.0625, 0.125, 0.0625,
    0.125,  0.25,  0.125,
    0.0625, 0.125, 0.0625
);

void main() {
    vec2 ps = 1.0 / uInputSize;

    // Map output pixel to input space
    vec2 srcPos = vTexCoord * uInputSize - 0.5;
    vec2 srcFloor = floor(srcPos);
    vec2 frac = srcPos - srcFloor;

    vec2 tc = (srcFloor + 0.5) * ps;
    vec2 dx = vec2(ps.x, 0.0);
    vec2 dy = vec2(0.0, ps.y);

    // ---- Step 1: Gather 3x3 neighborhood ----
    vec3 s[9];
    s[0] = texture(uInputTex, clamp(tc - dx - dy, vec2(0.0), vec2(1.0))).rgb;
    s[1] = texture(uInputTex, clamp(tc      - dy, vec2(0.0), vec2(1.0))).rgb;
    s[2] = texture(uInputTex, clamp(tc + dx - dy, vec2(0.0), vec2(1.0))).rgb;
    s[3] = texture(uInputTex, clamp(tc - dx,      vec2(0.0), vec2(1.0))).rgb;
    s[4] = texture(uInputTex,       tc                                  ).rgb;
    s[5] = texture(uInputTex, clamp(tc + dx,      vec2(0.0), vec2(1.0))).rgb;
    s[6] = texture(uInputTex, clamp(tc - dx + dy, vec2(0.0), vec2(1.0))).rgb;
    s[7] = texture(uInputTex, clamp(tc      + dy, vec2(0.0), vec2(1.0))).rgb;
    s[8] = texture(uInputTex, clamp(tc + dx + dy, vec2(0.0), vec2(1.0))).rgb;

    // ---- Step 2: Compute luma and Sobel gradient ----
    float l[9];
    for (int i = 0; i < 9; i++) {
        l[i] = luma(s[i]);
    }

    // Sobel-X: [-1 0 +1; -2 0 +2; -1 0 +1]
    float gx = -l[0] + l[2] - 2.0*l[3] + 2.0*l[5] - l[6] + l[8];
    // Sobel-Y: [-1 -2 -1; 0 0 0; +1 +2 +1]
    float gy = -l[0] - 2.0*l[1] - l[2] + l[6] + 2.0*l[7] + l[8];

    float gradMag = sqrt(gx * gx + gy * gy);

    // ---- Step 3: Classify edge direction (4 variants) ----
    // Use gradient angle to determine dominant edge direction
    float angle = atan(gy, gx); // gradient direction
    if (angle < 0.0) angle += 3.14159265;

    // Quantize to 4 directions
    int dirIdx;
    if (angle < 0.3927) {          // ~0 deg -> horizontal gradient -> vertical edge
        dirIdx = 2;
    } else if (angle < 1.1781) {   // ~45 deg -> diagonal gradient -> 135-deg edge
        dirIdx = 3;
    } else if (angle < 1.9635) {   // ~90 deg -> vertical gradient -> horizontal edge
        dirIdx = 0;
    } else if (angle < 2.7489) {   // ~135 deg -> diagonal gradient -> 45-deg edge
        dirIdx = 1;
    } else {                       // ~180 deg -> wraps to horizontal gradient
        dirIdx = 2;
    }

    // ---- Step 4: Select filter matrix and blend with flat filter ----
    // Edge strength determines blend between directional and flat filters
    float edgeBlend = smoothstep(0.03, 0.20, gradMag);

    float dirFilter[9];
    if (dirIdx == 0) {
        for (int i = 0; i < 9; i++) dirFilter[i] = H_FILTER[i];
    } else if (dirIdx == 1) {
        for (int i = 0; i < 9; i++) dirFilter[i] = D45_FILTER[i];
    } else if (dirIdx == 2) {
        for (int i = 0; i < 9; i++) dirFilter[i] = V_FILTER[i];
    } else {
        for (int i = 0; i < 9; i++) dirFilter[i] = D135_FILTER[i];
    }

    // Adapt weights based on sub-pixel position
    // Shift weight toward the nearest input pixel based on fractional position
    float subPixelBias[9];
    subPixelBias[0] = (1.0 - frac.x) * (1.0 - frac.y);
    subPixelBias[1] = (1.0 - frac.y);
    subPixelBias[2] = frac.x * (1.0 - frac.y);
    subPixelBias[3] = (1.0 - frac.x);
    subPixelBias[4] = 1.0;
    subPixelBias[5] = frac.x;
    subPixelBias[6] = (1.0 - frac.x) * frac.y;
    subPixelBias[7] = frac.y;
    subPixelBias[8] = frac.x * frac.y;

    // ---- Step 5: M x V — the actual matrix-vector multiply ----
    // Final weight = blend(flat, directional) * subPixelBias
    vec3 result = vec3(0.0);
    float wSum = 0.0;
    for (int i = 0; i < 9; i++) {
        float baseW = mix(FLAT_FILTER[i], dirFilter[i], edgeBlend);
        float w = baseW * subPixelBias[i];
        result += s[i] * w;
        wSum += w;
    }
    result /= max(wSum, 0.001);

    // ---- Step 6: Anti-halo clamp ----
    // Clamp to min/max of the 4 nearest input pixels
    vec3 localMin = min(min(s[4], s[5]), min(s[7], s[8]));
    vec3 localMax = max(max(s[4], s[5]), max(s[7], s[8]));
    // Widen slightly for sub-pixel positions near corners
    vec3 wideMin = min(localMin, min(min(s[0], s[2]), min(s[6], s[8])));
    vec3 wideMax = max(localMax, max(max(s[0], s[2]), max(s[6], s[8])));
    vec3 clampMin = mix(localMin, wideMin, 0.3);
    vec3 clampMax = mix(localMax, wideMax, 0.3);
    result = clamp(result, clampMin, clampMax);

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""
    }
}
