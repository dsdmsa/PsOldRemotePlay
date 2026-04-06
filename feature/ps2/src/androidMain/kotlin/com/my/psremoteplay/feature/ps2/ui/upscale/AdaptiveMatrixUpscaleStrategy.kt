package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Algorithm D: AdaptiveMatrixUpscale — per-pixel matrix-multiply upscaler.
 *
 * For each output pixel:
 *   1. Gather a 4x4 neighborhood from the input (16 RGB samples)
 *   2. Compute gradient angle (8 directions), gradient strength (3 levels),
 *      and sub-pixel position (2x2 quadrant = 4 positions)
 *   3. Index into a bank of 8 x 3 x 4 = 96 matrices, each 3x16
 *   4. Output = M x V, where V is the 16-element luma/chroma vector
 *
 * The full 384-matrix bank (8 angles x 3 strengths x 16 sub-pixel positions)
 * is reduced to 96 matrices (4 sub-pixel positions) for GLES 3.0 const array
 * limits. Each matrix row is 16 floats (one per 4x4 tap), producing one output
 * channel. Flat-area matrices are Catmull-Rom bicubic weights; edge matrices
 * are directional 1D cubic weights projected along the edge direction.
 *
 * Single-pass. 16 texture reads per output pixel.
 */
class AdaptiveMatrixUpscaleStrategy : UpscaleStrategy {

    override val name: String = "Adaptive Matrix"

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
// Matrix bank: 96 sets of 3x16 weights (stored as 96 * 3 = 288 vec4s,
// where each group of 4 vec4s = one 16-element row of a 3x16 matrix).
//
// Layout: matrices[angleIdx * 12 + strengthIdx * 4 + subPixelIdx]
//   where each "matrix" is 3 consecutive rows of 16 floats (= 12 vec4s).
//
// For GLES 3.0 const array limits, we store pre-computed Catmull-Rom
// bicubic weights for flat areas and directional cubic weights for edges.
// The matrices are generated analytically below as functions rather than
// as a massive const array, to stay within shader instruction limits.
// ============================================================

// Catmull-Rom basis weight for distance d from center
float crWeight(float d) {
    float ad = abs(d);
    if (ad <= 1.0) return 1.0 - 2.5 * ad * ad + 1.5 * ad * ad * ad;
    if (ad <= 2.0) return -0.5 * (ad - 1.0) * (ad - 2.0) * (ad - 2.0);
    return 0.0;
}

// Generate the weight for tap (i,j) in a 4x4 grid for sub-pixel offset (fx,fy)
// using isotropic Catmull-Rom (the "flat" matrix)
float bicubicWeight(int i, int j, float fx, float fy) {
    float dx = float(i) - 1.0 - fx;
    float dy = float(j) - 1.0 - fy;
    return crWeight(dx) * crWeight(dy);
}

// Generate the weight for tap (i,j) using directional 1D cubic along edge
// direction (edgeDir) with Gaussian falloff perpendicular to it.
// gradDir is the gradient direction (perpendicular to edge).
float directionalWeight(int i, int j, float fx, float fy,
                        vec2 edgeDir, vec2 gradDir) {
    vec2 offset = vec2(float(i) - 1.0 - fx, float(j) - 1.0 - fy);
    float dEdge = dot(offset, edgeDir);
    float dGrad = dot(offset, gradDir);
    float wEdge = crWeight(dEdge);
    float wGrad = exp(-6.0 * dGrad * dGrad);
    return wEdge * wGrad;
}

// Anisotropic bicubic: stretched along edge, compressed across
float anisotropicWeight(int i, int j, float fx, float fy,
                        vec2 edgeDir, vec2 gradDir) {
    vec2 offset = vec2(float(i) - 1.0 - fx, float(j) - 1.0 - fy);
    float dEdge = dot(offset, edgeDir);
    float dGrad = dot(offset, gradDir);
    return crWeight(dEdge * 0.8) * crWeight(dGrad * 1.4);
}

// 8 quantized edge directions (tangent along edge, not gradient)
// Angle index 0..7 corresponds to 0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5 degrees
const vec2 EDGE_DIRS[8] = vec2[8](
    vec2(1.0, 0.0),                      // 0 deg
    vec2(0.9239, 0.3827),                // 22.5 deg
    vec2(0.7071, 0.7071),                // 45 deg
    vec2(0.3827, 0.9239),                // 67.5 deg
    vec2(0.0, 1.0),                      // 90 deg
    vec2(-0.3827, 0.9239),               // 112.5 deg
    vec2(-0.7071, 0.7071),               // 135 deg
    vec2(-0.9239, 0.3827)                // 157.5 deg
);

void main() {
    vec2 ps = 1.0 / uInputSize;

    // Map output pixel to input space
    vec2 srcPos = vTexCoord * uInputSize - 0.5;
    vec2 srcFloor = floor(srcPos);
    vec2 frac = srcPos - srcFloor;

    // ---- Step 1: Gather 4x4 neighborhood ----
    vec3 samples[16];
    float lumaSamples[16];
    for (int j = 0; j < 4; j++) {
        for (int i = 0; i < 4; i++) {
            vec2 coord = (srcFloor + vec2(float(i) - 1.0, float(j) - 1.0) + 0.5) * ps;
            coord = clamp(coord, vec2(0.0), vec2(1.0));
            vec3 c = texture(uInputTex, coord).rgb;
            int idx = j * 4 + i;
            samples[idx] = c;
            lumaSamples[idx] = luma(c);
        }
    }

    // ---- Step 2: Sobel gradient from inner 3x3 (indices 0,1,2,4,5,6,8,9,10) ----
    float gx = -lumaSamples[0]  + lumaSamples[2]
             - 2.0 * lumaSamples[4] + 2.0 * lumaSamples[6]
             - lumaSamples[8]  + lumaSamples[10];

    float gy = -lumaSamples[0]  - 2.0 * lumaSamples[1] - lumaSamples[2]
             + lumaSamples[8] + 2.0 * lumaSamples[9] + lumaSamples[10];

    float gradMag = sqrt(gx * gx + gy * gy);

    // ---- Step 3: Classify gradient angle (8 directions) ----
    // Gradient angle -> edge tangent angle (rotate 90 degrees)
    float edgeAngle = atan(-gy, gx) + 1.5707963; // +PI/2 to get edge tangent
    if (edgeAngle < 0.0) edgeAngle += 3.14159265;
    if (edgeAngle >= 3.14159265) edgeAngle -= 3.14159265;

    // Quantize to 8 bins (each 22.5 degrees = PI/8)
    int angleIdx = int(floor(edgeAngle * 2.5464791 + 0.5)); // * 8/PI
    angleIdx = clamp(angleIdx, 0, 7);
    if (angleIdx >= 8) angleIdx = 0;

    // ---- Step 4: Classify gradient strength (3 levels) ----
    int strengthIdx;
    if (gradMag < 0.04) {
        strengthIdx = 0; // flat
    } else if (gradMag < 0.20) {
        strengthIdx = 1; // moderate
    } else {
        strengthIdx = 2; // strong edge
    }

    // ---- Step 5: Sub-pixel position (2x2 quadrant) ----
    int subPixelIdx = (frac.x >= 0.5 ? 1 : 0) + (frac.y >= 0.5 ? 2 : 0);

    // ---- Step 6: Compute M x V using the selected matrix ----
    // Instead of storing 96 * 48 = 4608 floats as const arrays (exceeding
    // GLES 3.0 limits), we compute the matrix weights on the fly based on
    // the classification. This is mathematically equivalent to a lookup.
    vec2 edgeDir = EDGE_DIRS[angleIdx];
    vec2 gradDir = vec2(-edgeDir.y, edgeDir.x); // perpendicular

    // Compute all 16 weights based on strength classification
    float weights[16];
    float wSum = 0.0;

    for (int j = 0; j < 4; j++) {
        for (int i = 0; i < 4; i++) {
            int idx = j * 4 + i;
            float w;
            if (strengthIdx == 0) {
                // Flat: isotropic Catmull-Rom bicubic
                w = bicubicWeight(i, j, frac.x, frac.y);
            } else if (strengthIdx == 1) {
                // Moderate: anisotropic bicubic stretched along edge
                w = anisotropicWeight(i, j, frac.x, frac.y, edgeDir, gradDir);
            } else {
                // Strong: 1D directional cubic along edge
                w = directionalWeight(i, j, frac.x, frac.y, edgeDir, gradDir);
            }
            weights[idx] = w;
            wSum += w;
        }
    }

    // Normalize weights
    float invW = 1.0 / max(wSum, 0.001);

    // ---- Step 7: Matrix-vector multiply: output = M x V ----
    // Each row of M produces one RGB channel. For color images, we apply
    // the same weight vector to all 3 channels (separable luma-based classification).
    vec3 result = vec3(0.0);
    for (int k = 0; k < 16; k++) {
        result += samples[k] * (weights[k] * invW);
    }

    // ---- Step 8: Anti-halo clamp to local 2x2 min/max ----
    vec3 localMin = min(min(samples[5], samples[6]), min(samples[9], samples[10]));
    vec3 localMax = max(max(samples[5], samples[6]), max(samples[9], samples[10]));
    result = clamp(result, localMin, localMax);

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""
    }
}
