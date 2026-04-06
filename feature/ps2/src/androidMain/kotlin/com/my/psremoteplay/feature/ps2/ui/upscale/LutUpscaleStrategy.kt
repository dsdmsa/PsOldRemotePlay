package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Pre-baked interpolation weight lookup upscaler.
 *
 * Single-pass strategy that uses pre-computed optimal weights (embedded as
 * const arrays in the shader) indexed by quantized sub-pixel position.
 * Weights combine standard bicubic interpolation with edge-aware correction
 * that adjusts the filter to favor samples along detected edges.
 *
 * The "lookup table" is fully embedded in the shader — no external textures needed.
 */
class LutUpscaleStrategy : UpscaleStrategy {

    override val name: String = "LUT Bicubic"

    override val isSinglePass: Boolean = true

    override val upscaleFragShader: String
        get() = LUT_FRAG.trimIndent().trim()

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
        private const val LUT_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;
uniform vec2 uOutputSize;

in vec2 vTexCoord;
out vec4 fragColor;

// Pre-baked bicubic weights for 4 sub-pixel positions along each axis.
// Indexed as weights[subPixelIdx][tap], where tap 0..3 maps to samples -1..+2.
// These are Catmull-Rom weights evaluated at fractional positions 0.125, 0.375, 0.625, 0.875.
const float LUT_W[4][4] = float[4][4](
    // fx/fy ~ 0.125 (close to left/top sample)
    float[4](-0.0522, 0.9399, 0.1230, -0.0107),
    // fx/fy ~ 0.375
    float[4](-0.0898, 0.7148, 0.4199, -0.0449),
    // fx/fy ~ 0.625
    float[4](-0.0449, 0.4199, 0.7148, -0.0898),
    // fx/fy ~ 0.875 (close to right/bottom sample)
    float[4](-0.0107, 0.1230, 0.9399, -0.0522)
);

// Edge-aware correction offsets per sub-pixel position.
// Applied to shift weight toward samples along the detected edge.
// [subPixelIdx][tap] — added to base weights when edge is detected.
const float EDGE_CORR[4][4] = float[4][4](
    float[4]( 0.015, -0.030,  0.020, -0.005),
    float[4]( 0.020, -0.045,  0.035, -0.010),
    float[4](-0.010,  0.035, -0.045,  0.020),
    float[4](-0.005,  0.020, -0.030,  0.015)
);

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec2 invInput = 1.0 / uInputSize;

    // Map output texcoord to input space
    vec2 srcPos = vTexCoord * uInputSize - 0.5;
    vec2 srcFloor = floor(srcPos);
    vec2 frac = srcPos - srcFloor;

    // Quantize sub-pixel offset into 4x4 = 16 positions
    int qx = clamp(int(frac.x * 4.0), 0, 3);
    int qy = clamp(int(frac.y * 4.0), 0, 3);

    // Fetch 4x4 neighborhood
    vec3 samples[16];
    float lumaSamples[16];
    for (int j = 0; j < 4; j++) {
        for (int i = 0; i < 4; i++) {
            vec2 coord = (srcFloor + vec2(float(i) - 1.0, float(j) - 1.0) + 0.5) * invInput;
            coord = clamp(coord, vec2(0.0), vec2(1.0));
            vec3 c = texture(uInputTex, coord).rgb;
            int idx = j * 4 + i;
            samples[idx] = c;
            lumaSamples[idx] = luma(c);
        }
    }

    // Detect local edge direction from 2x2 luma differences around center
    // Center 2x2: indices 5(1,1), 6(2,1), 9(1,2), 10(2,2)
    float dH = (lumaSamples[6] + lumaSamples[10]) - (lumaSamples[5] + lumaSamples[9]);
    float dV = (lumaSamples[9] + lumaSamples[10]) - (lumaSamples[5] + lumaSamples[6]);
    float dD1 = lumaSamples[10] - lumaSamples[5];  // diagonal 1
    float dD2 = lumaSamples[9] - lumaSamples[6];   // diagonal 2

    // Edge strength and dominant direction
    float edgeH = abs(dV);  // horizontal edge detected by vertical difference
    float edgeV = abs(dH);  // vertical edge detected by horizontal difference
    float edgeStrength = max(edgeH, edgeV);

    // Determine if edge is more horizontal or vertical
    // edgeFactor: positive = horizontal edge (adjust Y weights), negative = vertical (adjust X weights)
    float edgeFactor = clamp(edgeStrength * 4.0, 0.0, 1.0);
    bool horizEdge = edgeH > edgeV;

    // Look up base weights from LUT
    float wx[4];
    float wy[4];
    for (int i = 0; i < 4; i++) {
        wx[i] = LUT_W[qx][i];
        wy[i] = LUT_W[qy][i];
    }

    // Apply edge-aware correction
    // For horizontal edges: adjust vertical (Y) weights to follow edge
    // For vertical edges: adjust horizontal (X) weights to follow edge
    if (horizEdge) {
        for (int i = 0; i < 4; i++) {
            wy[i] += EDGE_CORR[qy][i] * edgeFactor;
        }
    } else {
        for (int i = 0; i < 4; i++) {
            wx[i] += EDGE_CORR[qx][i] * edgeFactor;
        }
    }

    // Normalize weights
    float sumX = wx[0] + wx[1] + wx[2] + wx[3];
    float sumY = wy[0] + wy[1] + wy[2] + wy[3];
    float invSumX = 1.0 / sumX;
    float invSumY = 1.0 / sumY;
    for (int i = 0; i < 4; i++) {
        wx[i] *= invSumX;
        wy[i] *= invSumY;
    }

    // Apply separable weighted sum
    vec3 result = vec3(0.0);
    for (int j = 0; j < 4; j++) {
        vec3 rowSum = vec3(0.0);
        for (int i = 0; i < 4; i++) {
            rowSum += samples[j * 4 + i] * wx[i];
        }
        result += rowSum * wy[j];
    }

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""
    }
}
