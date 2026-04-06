package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Qualcomm Snapdragon Game Super Resolution (SGSR 1) — single-pass upscaler.
 *
 * Uses textureGather for efficient 12-tap sampling (4 gather calls vs 12 texture calls).
 * Early-exits for non-edge pixels to save GPU cycles on flat areas.
 * Designed for mobile GPUs from the ground up.
 *
 * Note: Requires textureGather (GLES 3.1+ on some devices).
 */
class SgsrStrategy : UpscaleStrategy {

    override val name: String = "Snapdragon GSR"

    override val isSinglePass: Boolean = true

    override val upscaleFragShader: String
        get() = SGSR_FRAG.trimIndent().trim()

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
        val sharpnessLoc = GLES30.glGetUniformLocation(program, "uEdgeSharpness")
        GLES30.glUniform1i(texLoc, 0)
        GLES30.glUniform2f(inputSizeLoc, inputWidth.toFloat(), inputHeight.toFloat())
        GLES30.glUniform2f(outputSizeLoc, outputWidth.toFloat(), outputHeight.toFloat())
        GLES30.glUniform1f(sharpnessLoc, DEFAULT_EDGE_SHARPNESS)
    }

    companion object {
        const val DEFAULT_EDGE_SHARPNESS = 2.0f

        private const val SGSR_FRAG = """
#version 300 es
precision mediump float;
precision highp int;

uniform highp vec2 uInputSize;   // e.g., 640.0, 448.0
uniform highp vec2 uOutputSize;  // e.g., 1720.0, 1080.0
uniform sampler2D uInputTex;
uniform float uEdgeSharpness;    // default: 2.0

in vec2 vTexCoord;
out vec4 fragColor;

#define EdgeThreshold (8.0 / 255.0)

float fastLanczos2(float x) {
    float wA = x - 4.0;
    float wB = x * wA - wA;
    wA *= wA;
    return wB * wA;
}

vec2 weightY(float dx, float dy, float c, vec3 data) {
    float std = data.x;
    vec2 dir = data.yz;

    float edgeDis = dx * dir.y + dy * dir.x;
    float x = (dx * dx + dy * dy) +
              edgeDis * edgeDis * (clamp(c * c * std, 0.0, 1.0) * 0.7 - 1.0);

    float w = fastLanczos2(x);
    return vec2(w, w * c);
}

vec2 edgeDirection(vec4 left, vec4 right) {
    float RxLz = right.x - left.z;
    float RwLy = right.w - left.y;
    vec2 delta;
    delta.x = RxLz + RwLy;
    delta.y = RxLz - RwLy;
    float lengthInv = inversesqrt(delta.x * delta.x + 3.076e-05 + delta.y * delta.y);
    return delta * lengthInv;
}

void main() {
    vec3 color = textureLod(uInputTex, vTexCoord, 0.0).rgb;

    highp vec2 viewportInvSize = 1.0 / uOutputSize;  // texel size in output space
    highp vec2 viewportSize = uOutputSize;

    highp vec2 imgCoord = vTexCoord * viewportSize + vec2(-0.5, 0.5);
    highp vec2 imgCoordPixel = floor(imgCoord);
    highp vec2 coord = imgCoordPixel * viewportInvSize;
    vec2 pl = imgCoord - imgCoordPixel;

    // Green channel used for luma (fast approximation)
    vec4 left = textureGather(uInputTex, coord, 1);

    float edgeVote = abs(left.z - left.y) + abs(color.g - left.y) + abs(color.g - left.z);

    if (edgeVote > EdgeThreshold) {
        coord.x += viewportInvSize.x;

        vec4 right = textureGather(uInputTex, coord + vec2(viewportInvSize.x, 0.0), 1);
        vec4 upDown;
        upDown.xy = textureGather(uInputTex, coord + vec2(0.0, -viewportInvSize.y), 1).wz;
        upDown.zw = textureGather(uInputTex, coord + vec2(0.0, viewportInvSize.y), 1).yx;

        float mean = (left.y + left.z + right.x + right.w) * 0.25;
        left -= vec4(mean);
        right -= vec4(mean);
        upDown -= vec4(mean);
        float cw = color.g - mean;

        float sum = abs(left.x) + abs(left.y) + abs(left.z) + abs(left.w) +
                    abs(right.x) + abs(right.y) + abs(right.z) + abs(right.w) +
                    abs(upDown.x) + abs(upDown.y) + abs(upDown.z) + abs(upDown.w);
        float sumMean = 10.14185 / sum;
        float std = sumMean * sumMean;

        vec3 data = vec3(std, edgeDirection(left, right));

        vec2 aWY = weightY(pl.x, pl.y + 1.0, upDown.x, data);
        aWY += weightY(pl.x - 1.0, pl.y + 1.0, upDown.y, data);
        aWY += weightY(pl.x - 1.0, pl.y - 2.0, upDown.z, data);
        aWY += weightY(pl.x, pl.y - 2.0, upDown.w, data);
        aWY += weightY(pl.x + 1.0, pl.y - 1.0, left.x, data);
        aWY += weightY(pl.x, pl.y - 1.0, left.y, data);
        aWY += weightY(pl.x, pl.y, left.z, data);
        aWY += weightY(pl.x + 1.0, pl.y, left.w, data);
        aWY += weightY(pl.x - 1.0, pl.y - 1.0, right.x, data);
        aWY += weightY(pl.x - 2.0, pl.y - 1.0, right.y, data);
        aWY += weightY(pl.x - 2.0, pl.y, right.z, data);
        aWY += weightY(pl.x - 1.0, pl.y, right.w, data);

        float finalY = aWY.y / aWY.x;
        float maxY = max(max(left.y, left.z), max(right.x, right.w));
        float minY = min(min(left.y, left.z), min(right.x, right.w));
        float deltaY = clamp(uEdgeSharpness * finalY, minY, maxY) - cw;

        // Smooth high contrast to prevent artifacts
        deltaY = clamp(deltaY, -23.0 / 255.0, 23.0 / 255.0);

        color = clamp(color + vec3(deltaY), 0.0, 1.0);
    }

    fragColor = vec4(color, 1.0);
}
"""
    }
}
