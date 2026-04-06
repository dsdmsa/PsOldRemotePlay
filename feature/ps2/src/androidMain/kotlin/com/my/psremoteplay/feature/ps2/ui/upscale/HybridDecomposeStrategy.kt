package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Algorithm F: HybridDecompose — multi-matrix decomposition upscaler.
 *
 * Applies 3 "filter matrices" to the same 5x5 neighborhood simultaneously:
 *   M_edge   (2x25): extracts gradient vector (Gx, Gy)
 *   M_smooth (3x25): computes bilateral-smoothed upscaled RGB
 *   M_detail (3x25): extracts high-frequency detail RGB
 *
 * Then combines: output = smooth + detail * f(edge_magnitude)
 *
 * Where f() is a sigmoid that boosts detail on edges (sharpening) and
 * suppresses detail in flat areas (denoising).
 *
 * All three matrices are applied in a single pass over the 5x5 neighborhood
 * (25 texture reads). The matrices are stored as const arrays.
 *
 * This is the mathematical formalization of: decompose the image into
 * structure and detail layers, upscale each differently, recombine with
 * edge-awareness.
 *
 * Single-pass. 25 texture reads per output pixel.
 */
class HybridDecomposeStrategy : UpscaleStrategy {

    override val name: String = "Hybrid Decompose"

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

// Catmull-Rom weight for distance d
float crWeight(float d) {
    float ad = abs(d);
    if (ad <= 1.0) return 1.0 - 2.5 * ad * ad + 1.5 * ad * ad * ad;
    if (ad <= 2.0) return -0.5 * (ad - 1.0) * (ad - 2.0) * (ad - 2.0);
    return 0.0;
}

// ============================================================
// M_edge: 2x25 matrix (Sobel-like gradient extraction)
//
// Extended 5x5 Scharr-like gradient operator for better rotation
// invariance than standard 3x3 Sobel. Stored as const arrays.
//
// Layout: 5x5 grid, row-major, index = y*5 + x
//   (-2,-2) (-1,-2) (0,-2) (1,-2) (2,-2)
//   (-2,-1) (-1,-1) (0,-1) (1,-1) (2,-1)
//   (-2, 0) (-1, 0) (0, 0) (1, 0) (2, 0)
//   (-2, 1) (-1, 1) (0, 1) (1, 1) (2, 1)
//   (-2, 2) (-1, 2) (0, 2) (1, 2) (2, 2)
// ============================================================

// Extended Sobel-X (5x5)
const float EDGE_X[25] = float[25](
   -1.0, -2.0,  0.0,  2.0,  1.0,
   -4.0, -8.0,  0.0,  8.0,  4.0,
   -6.0,-12.0,  0.0, 12.0,  6.0,
   -4.0, -8.0,  0.0,  8.0,  4.0,
   -1.0, -2.0,  0.0,  2.0,  1.0
);

// Extended Sobel-Y (5x5)
const float EDGE_Y[25] = float[25](
   -1.0, -4.0, -6.0, -4.0, -1.0,
   -2.0, -8.0,-12.0, -8.0, -2.0,
    0.0,  0.0,  0.0,  0.0,  0.0,
    2.0,  8.0, 12.0,  8.0,  2.0,
    1.0,  4.0,  6.0,  4.0,  1.0
);

// Normalization factor for the 5x5 Sobel (sum of absolute values / 2)
const float EDGE_NORM = 1.0 / 96.0;

// ============================================================
// M_smooth: 3x25 matrix (bilateral-smoothed upscale)
//
// Combines bicubic interpolation with bilateral (luma-similarity)
// weighting. The spatial part is Catmull-Rom; the range part is
// a Gaussian on luma difference. This is computed dynamically
// since the bilateral weights depend on the pixel values.
// ============================================================

// ============================================================
// M_detail: 3x25 matrix (high-frequency detail extraction)
//
// Computed as: detail = identity - lowpass
// The lowpass is a normalized 5x5 Gaussian (sigma=1.0).
// Detail at center pixel = center_sample - gaussian_smoothed.
// This is applied per-channel to preserve color detail.
// ============================================================

// 5x5 Gaussian kernel (sigma=1.0), NOT normalized
const float GAUSS_5x5[25] = float[25](
    0.0030, 0.0133, 0.0219, 0.0133, 0.0030,
    0.0133, 0.0596, 0.0983, 0.0596, 0.0133,
    0.0219, 0.0983, 0.1621, 0.0983, 0.0219,
    0.0133, 0.0596, 0.0983, 0.0596, 0.0133,
    0.0030, 0.0133, 0.0219, 0.0133, 0.0030
);

// Detail boost function: sigmoid that maps edge magnitude to detail weight.
// At strong edges: preserve detail (output ~1.0).
// At flat areas: suppress detail (output ~0.2) to reduce noise.
float detailWeight(float edgeMag) {
    // Tuned for PS2 content (480i/p upscaled to 720p/1080p)
    float x = (edgeMag - 0.08) * 15.0;
    return 0.2 + 0.8 / (1.0 + exp(-x));
}

void main() {
    vec2 ps = 1.0 / uInputSize;

    // Map output pixel to input space
    vec2 srcPos = vTexCoord * uInputSize - 0.5;
    vec2 srcFloor = floor(srcPos);
    vec2 frac = srcPos - srcFloor;

    // ---- Step 1: Gather 5x5 neighborhood ----
    vec3 samples[25];
    float lumaS[25];
    for (int j = 0; j < 5; j++) {
        for (int i = 0; i < 5; i++) {
            vec2 coord = (srcFloor + vec2(float(i) - 2.0, float(j) - 2.0) + 0.5) * ps;
            coord = clamp(coord, vec2(0.0), vec2(1.0));
            vec3 c = texture(uInputTex, coord).rgb;
            int idx = j * 5 + i;
            samples[idx] = c;
            lumaS[idx] = luma(c);
        }
    }

    // Center pixel luma (index 12 = position (0,0) in the 5x5 grid)
    float centerLuma = lumaS[12];

    // ---- Step 2: M_edge x V — extract gradient vector ----
    float gx = 0.0;
    float gy = 0.0;
    for (int k = 0; k < 25; k++) {
        gx += EDGE_X[k] * lumaS[k];
        gy += EDGE_Y[k] * lumaS[k];
    }
    gx *= EDGE_NORM;
    gy *= EDGE_NORM;
    float edgeMag = sqrt(gx * gx + gy * gy);

    // Edge direction (tangent along edge, perpendicular to gradient)
    vec2 edgeDir = vec2(-gy, gx);
    float edgeLen = length(edgeDir);
    edgeDir = edgeLen > 1e-5 ? edgeDir / edgeLen : vec2(1.0, 0.0);
    vec2 gradDir = vec2(edgeDir.y, -edgeDir.x);

    // ---- Step 3: M_smooth x V — bilateral-smoothed upscale ----
    // Spatial weights: bicubic Catmull-Rom adapted for 5x5 grid
    // Range weights: Gaussian on luma difference from center
    vec3 smoothResult = vec3(0.0);
    float smoothWSum = 0.0;

    for (int j = 0; j < 5; j++) {
        for (int i = 0; i < 5; i++) {
            int idx = j * 5 + i;
            float dx = float(i) - 2.0 - frac.x;
            float dy = float(j) - 2.0 - frac.y;

            // Spatial weight: separable Catmull-Rom
            float wSpatial = crWeight(dx) * crWeight(dy);

            // Range weight: bilateral (luma similarity to center)
            float lumaDiff = lumaS[idx] - centerLuma;
            float wRange = exp(-lumaDiff * lumaDiff * 50.0);

            // Edge-adaptive: stretch spatial weight along edge direction
            vec2 offset = vec2(dx, dy);
            float dEdge = dot(offset, edgeDir);
            float dGrad = dot(offset, gradDir);
            // Mild anisotropy: compress slightly across edge
            float aniso = 1.0 + 0.5 * smoothstep(0.05, 0.25, edgeMag);
            float wAniso = crWeight(dEdge) * crWeight(dGrad * aniso);

            // Blend isotropic and anisotropic based on edge strength
            float edgeFactor = smoothstep(0.03, 0.20, edgeMag);
            float wFinal = mix(wSpatial, wAniso, edgeFactor) * wRange;

            smoothResult += samples[idx] * wFinal;
            smoothWSum += wFinal;
        }
    }
    smoothResult /= max(smoothWSum, 0.001);

    // ---- Step 4: M_detail x V — extract high-frequency detail ----
    // Detail = center_interpolated - gaussian_smoothed
    // First compute Gaussian-smoothed value at sub-pixel position
    vec3 gaussSmoothed = vec3(0.0);
    float gaussSum = 0.0;
    for (int k = 0; k < 25; k++) {
        gaussSmoothed += samples[k] * GAUSS_5x5[k];
        gaussSum += GAUSS_5x5[k];
    }
    gaussSmoothed /= max(gaussSum, 0.001);

    // Detail is the difference between the bilateral result and Gaussian smooth
    // This captures edges and texture that the Gaussian blur would remove
    vec3 detail = smoothResult - gaussSmoothed;

    // Also compute a "sharp detail" from the center pixel's local contrast
    vec3 centerDetail = samples[12] - gaussSmoothed;

    // Blend the two detail sources based on sub-pixel distance to center
    float centerDist = length(frac - 0.5);
    float centerBlend = 1.0 - smoothstep(0.0, 0.7, centerDist);
    vec3 finalDetail = mix(detail, centerDetail, centerBlend * 0.5);

    // ---- Step 5: Combine: output = smooth + detail * f(edgeMag) ----
    float dw = detailWeight(edgeMag);
    vec3 result = smoothResult + finalDetail * dw;

    // ---- Step 6: Anti-halo clamp to local 3x3 min/max ----
    // Use inner 3x3 (indices 6,7,8, 11,12,13, 16,17,18)
    vec3 localMin = samples[12];
    vec3 localMax = samples[12];
    localMin = min(localMin, min(samples[6],  samples[7]));
    localMin = min(localMin, min(samples[8],  samples[11]));
    localMin = min(localMin, min(samples[13], samples[16]));
    localMin = min(localMin, min(samples[17], samples[18]));
    localMax = max(localMax, max(samples[6],  samples[7]));
    localMax = max(localMax, max(samples[8],  samples[11]));
    localMax = max(localMax, max(samples[13], samples[16]));
    localMax = max(localMax, max(samples[17], samples[18]));

    result = clamp(result, localMin, localMax);

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""
    }
}
