package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * RAISR-style learned filter bank upscaler (hand-crafted variant).
 *
 * Single-pass strategy that classifies each pixel's local structure by
 * gradient angle (4 directions) and gradient strength (3 levels), then
 * selects one of 12 specialised interpolation filters:
 *   - Weak edges:   standard bicubic (isotropic)
 *   - Medium edges:  bicubic stretched along edge direction (anisotropic)
 *   - Strong edges:  1D cubic interpolation purely along edge direction
 *
 * Output is clamped to the local 2x2 min/max to prevent ringing.
 */
class RaisrStrategy : UpscaleStrategy {

    override val name: String = "RAISR"

    override val isSinglePass: Boolean = true

    override val upscaleFragShader: String
        get() = RAISR_FRAG.trimIndent().trim()

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
        private const val RAISR_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;
uniform vec2 uOutputSize;

in vec2 vTexCoord;
out vec4 fragColor;

// Luma from RGB (BT.709)
float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// Standard cubic basis (Catmull-Rom, alpha = -0.5)
float cubic(float x) {
    float ax = abs(x);
    if (ax <= 1.0) return 1.0 - 2.5 * ax * ax + 1.5 * ax * ax * ax;
    if (ax <= 2.0) return -0.5 * (ax - 1.0) * (ax - 2.0) * (ax - 2.0);
    return 0.0;
}

void main() {
    vec2 invInput = 1.0 / uInputSize;

    // Map output texcoord to input space
    vec2 srcPos = vTexCoord * uInputSize - 0.5;
    vec2 srcFloor = floor(srcPos);
    vec2 frac = srcPos - srcFloor;

    // Fetch 4x4 neighborhood (colors and luma)
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

    // Compute Sobel gradient from 3x3 centered on the nearest input pixel
    // Using indices 5,6,9,10 as the center 2x2, and the 3x3 around index 5
    float gx = -lumaSamples[0]  + lumaSamples[2]
             - 2.0 * lumaSamples[4] + 2.0 * lumaSamples[6]
             - lumaSamples[8]  + lumaSamples[10];

    float gy = -lumaSamples[0]  - 2.0 * lumaSamples[1] - lumaSamples[2]
             + lumaSamples[8] + 2.0 * lumaSamples[9] + lumaSamples[10];

    // Gradient strength
    float gradStr = sqrt(gx * gx + gy * gy);

    // Quantize strength into 3 levels: 0=weak, 1=medium, 2=strong
    int strength;
    if (gradStr < 0.05) {
        strength = 0; // weak
    } else if (gradStr < 0.25) {
        strength = 1; // medium
    } else {
        strength = 2; // strong
    }

    // Gradient angle quantized to 4 directions
    float angle = atan(gy, gx); // -PI..PI
    // Normalize to 0..PI (edge direction is symmetric)
    if (angle < 0.0) angle += 3.14159265;

    int angleIdx;
    if (angle < 0.3927) {        // 0 deg (horizontal edge)
        angleIdx = 0;
    } else if (angle < 1.1781) { // 45 deg
        angleIdx = 1;
    } else if (angle < 1.9635) { // 90 deg (vertical edge)
        angleIdx = 2;
    } else if (angle < 2.7489) { // 135 deg
        angleIdx = 3;
    } else {                     // wrap to 0 deg
        angleIdx = 0;
    }

    // Edge direction unit vectors for anisotropic/1D filtering
    // These are along the edge (perpendicular to gradient)
    vec2 edgeDir;
    if (angleIdx == 0)      edgeDir = vec2(0.0, 1.0);  // horizontal edge -> sample vertically along edge
    else if (angleIdx == 1) edgeDir = vec2(-0.7071, 0.7071);
    else if (angleIdx == 2) edgeDir = vec2(1.0, 0.0);  // vertical edge -> sample horizontally along edge
    else                    edgeDir = vec2(0.7071, 0.7071);

    vec3 result;

    if (strength == 0) {
        // Weak edges: standard bicubic (isotropic Catmull-Rom)
        vec3 col = vec3(0.0);
        float wSum = 0.0;
        for (int j = 0; j < 4; j++) {
            float wy = cubic(float(j) - 1.0 - frac.y);
            for (int i = 0; i < 4; i++) {
                float wx = cubic(float(i) - 1.0 - frac.x);
                float w = wx * wy;
                col += samples[j * 4 + i] * w;
                wSum += w;
            }
        }
        result = col / wSum;

    } else if (strength == 1) {
        // Medium edges: anisotropic bicubic (stretched along edge direction)
        // Compress the coordinate along the gradient, expand along edge
        vec2 gradDir = (gradStr > 0.001) ? normalize(vec2(gx, gy)) : vec2(0.0, 1.0);
        vec2 edir = vec2(-gradDir.y, gradDir.x); // edge direction

        vec3 col = vec3(0.0);
        float wSum = 0.0;
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 4; i++) {
                vec2 offset = vec2(float(i) - 1.0 - frac.x, float(j) - 1.0 - frac.y);
                // Project onto edge and gradient directions
                float dEdge = dot(offset, edir);
                float dGrad = dot(offset, gradDir);
                // Stretch: halve the weight falloff along the edge, double along gradient
                float wx = cubic(dEdge * 0.75);
                float wy = cubic(dGrad * 1.5);
                float w = wx * wy;
                col += samples[j * 4 + i] * w;
                wSum += w;
            }
        }
        result = col / max(wSum, 0.001);

    } else {
        // Strong edges: 1D cubic interpolation purely along edge direction
        vec2 gradDir = (gradStr > 0.001) ? normalize(vec2(gx, gy)) : vec2(0.0, 1.0);
        vec2 edir = vec2(-gradDir.y, gradDir.x);

        // Project sub-pixel offset onto edge direction
        vec2 subOffset = vec2(frac.x, frac.y);
        float edgeFrac = dot(subOffset, edir);

        // Collect samples projected onto edge direction, sort by projection
        // Use all 16 samples but weight by proximity to edge line
        vec3 col = vec3(0.0);
        float wSum = 0.0;
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 4; i++) {
                vec2 offset = vec2(float(i) - 1.0 - frac.x, float(j) - 1.0 - frac.y);
                float dEdge = dot(offset, edir);
                float dGrad = dot(offset, gradDir);
                // 1D cubic along edge, gaussian falloff across edge (narrow)
                float wEdge = cubic(dEdge);
                float wGrad = exp(-8.0 * dGrad * dGrad);
                float w = wEdge * wGrad;
                col += samples[j * 4 + i] * w;
                wSum += w;
            }
        }
        result = col / max(wSum, 0.001);
    }

    // Clamp to local 2x2 min/max to prevent ringing
    vec3 localMin = min(min(samples[5], samples[6]), min(samples[9], samples[10]));
    vec3 localMax = max(max(samples[5], samples[6]), max(samples[9], samples[10]));
    result = clamp(result, localMin, localMax);

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""
    }
}
