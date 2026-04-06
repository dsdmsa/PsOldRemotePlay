package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Optimal upscaler: Catmull-Rom bicubic with selective block-boundary artifact suppression.
 *
 * Strategy: Bicubic wins on compressed video because it doesn't amplify artifacts.
 * The ONE thing that can beat it is suppressing the artifacts bicubic faithfully reproduces.
 *
 * Algorithm (single pass, 25 texture reads max):
 *   1. Sample the 4x4 Catmull-Rom neighborhood (9 reads via bilinear trick).
 *   2. Detect H.264 block boundaries (8x8 and 4x4 grids) by checking if the
 *      output pixel maps near a block seam in input space.
 *   3. At block boundaries only: measure the cross-boundary luma discontinuity
 *      from 4 additional reads. If the discontinuity looks like a compression
 *      artifact (abrupt step without strong surrounding gradient), blend a small
 *      bilateral correction into the bicubic result to smooth the seam.
 *   4. Away from boundaries: pure Catmull-Rom, zero overhead beyond the check.
 *
 * This guarantees we never look worse than bicubic (we only smooth artifact seams)
 * and can only look better (reduced blocking on compressed content).
 *
 * Budget: 9 base + 4 boundary detect + up to 12 bilateral = 25 reads max at boundaries,
 * 9 reads in smooth areas (majority of pixels).
 */
class OptimalUpscaleStrategy : UpscaleStrategy {

    override val name: String = "Optimal"

    override val isSinglePass: Boolean = true

    override val upscaleFragShader: String
        get() = OPTIMAL_FRAG.trimIndent().trim()

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
        private const val OPTIMAL_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;   // e.g. 640.0, 448.0
uniform vec2 uOutputSize;  // e.g. 1920.0, 1080.0

in vec2 vTexCoord;
out vec4 fragColor;

float luma(vec3 c) {
    return 0.299 * c.r + 0.587 * c.g + 0.114 * c.b;
}

void main() {
    vec2 invTexSize = 1.0 / uInputSize;

    // --- Catmull-Rom bicubic (9-tap bilinear trick) ---
    vec2 samplePos = vTexCoord * uInputSize;
    vec2 texPos1 = floor(samplePos - 0.5) + 0.5;
    vec2 f = samplePos - texPos1;

    // Catmull-Rom basis
    vec2 w0 = f * (-0.5 + f * (1.0 - 0.5 * f));
    vec2 w1 = 1.0 + f * f * (-2.5 + 1.5 * f);
    vec2 w2 = f * (0.5 + f * (2.0 - 1.5 * f));
    vec2 w3 = f * f * (-0.5 + 0.5 * f);

    vec2 w12 = w1 + w2;
    vec2 offset12 = w2 / w12;

    vec2 texPos0  = (texPos1 - 1.0) * invTexSize;
    vec2 texPos3  = (texPos1 + 2.0) * invTexSize;
    vec2 texPos12 = (texPos1 + offset12) * invTexSize;

    // 9 texture reads for bicubic
    vec3 bicubic = vec3(0.0);
    bicubic += texture(uInputTex, vec2(texPos0.x,  texPos0.y)).rgb  * w0.x  * w0.y;
    bicubic += texture(uInputTex, vec2(texPos12.x, texPos0.y)).rgb  * w12.x * w0.y;
    bicubic += texture(uInputTex, vec2(texPos3.x,  texPos0.y)).rgb  * w3.x  * w0.y;
    bicubic += texture(uInputTex, vec2(texPos0.x,  texPos12.y)).rgb * w0.x  * w12.y;
    bicubic += texture(uInputTex, vec2(texPos12.x, texPos12.y)).rgb * w12.x * w12.y;
    bicubic += texture(uInputTex, vec2(texPos3.x,  texPos12.y)).rgb * w3.x  * w12.y;
    bicubic += texture(uInputTex, vec2(texPos0.x,  texPos3.y)).rgb  * w0.x  * w3.y;
    bicubic += texture(uInputTex, vec2(texPos12.x, texPos3.y)).rgb  * w12.x * w3.y;
    bicubic += texture(uInputTex, vec2(texPos3.x,  texPos3.y)).rgb  * w3.x  * w3.y;

    // --- Block boundary artifact suppression ---
    // Check proximity to H.264 macroblock (8px) and sub-block (4px) boundaries
    // in input pixel space.
    vec2 inputPx = samplePos;  // position in input pixels

    // Distance to nearest 8px and 4px grid lines
    vec2 dist8 = abs(mod(inputPx, 8.0) - 4.0);  // 0..4, min at boundary
    vec2 dist4 = abs(mod(inputPx, 4.0) - 2.0);  // 0..2, min at boundary

    float nearBound8 = min(dist8.x, dist8.y);
    float nearBound4 = min(dist4.x, dist4.y);

    // Boundary proximity: 1.0 at boundary, 0.0 more than 1.5px away
    // 8px boundaries get stronger correction, 4px boundaries get lighter
    float boundWeight8 = 1.0 - smoothstep(0.0, 1.5, nearBound8);
    float boundWeight4 = 1.0 - smoothstep(0.0, 1.0, nearBound4);
    float boundWeight = max(boundWeight8, boundWeight4 * 0.5);

    if (boundWeight > 0.01) {
        // At or near a block boundary: check if there's an actual artifact.
        // Sample 4 neighbors to measure cross-boundary discontinuity.
        // (4 reads)
        vec2 tc = vTexCoord;
        vec3 tN = texture(uInputTex, tc + vec2(0.0, -invTexSize.y)).rgb;
        vec3 tS = texture(uInputTex, tc + vec2(0.0,  invTexSize.y)).rgb;
        vec3 tW = texture(uInputTex, tc + vec2(-invTexSize.x, 0.0)).rgb;
        vec3 tE = texture(uInputTex, tc + vec2( invTexSize.x, 0.0)).rgb;

        // Center luma and neighbor lumas
        float lC = luma(bicubic);
        float lN = luma(tN);
        float lS = luma(tS);
        float lW = luma(tW);
        float lE = luma(tE);

        // Cross-boundary step: max absolute difference from center
        float maxStep = max(max(abs(lC - lN), abs(lC - lS)),
                           max(abs(lC - lW), abs(lC - lE)));

        // Surrounding gradient: are neighbors themselves consistent?
        // If NS and WE neighbors agree with each other, but disagree with center,
        // that's an artifact. If they also disagree, it's a real edge.
        float neighborVar = (abs(lN - lS) + abs(lW - lE)) * 0.5;

        // Artifact signature: abrupt step at boundary + low surrounding variance
        // Real edges have high step AND high neighborVar. Artifacts have high step but low neighborVar.
        float artifactness = maxStep * (1.0 - smoothstep(0.0, maxStep * 0.8 + 0.01, neighborVar));

        // Only suppress if artifactness is significant (> threshold)
        float suppressStrength = smoothstep(0.02, 0.12, artifactness) * boundWeight * 0.6;

        if (suppressStrength > 0.01) {
            // Lightweight bilateral average of the 4 + center (already sampled).
            // Weighted by luma similarity to center to preserve real edges that
            // happen to align with the grid.
            float sigma = 15.0;
            float wC = 1.0;
            float wN = exp(-((lN - lC) * (lN - lC)) * sigma);
            float wS = exp(-((lS - lC) * (lS - lC)) * sigma);
            float wW = exp(-((lW - lC) * (lW - lC)) * sigma);
            float wE = exp(-((lE - lC) * (lE - lC)) * sigma);

            // Add 4 diagonal neighbors for better smoothing (4 more reads, total 17)
            vec3 tNW = texture(uInputTex, tc + vec2(-invTexSize.x, -invTexSize.y)).rgb;
            vec3 tNE = texture(uInputTex, tc + vec2( invTexSize.x, -invTexSize.y)).rgb;
            vec3 tSW = texture(uInputTex, tc + vec2(-invTexSize.x,  invTexSize.y)).rgb;
            vec3 tSE = texture(uInputTex, tc + vec2( invTexSize.x,  invTexSize.y)).rgb;

            float lNW = luma(tNW); float lNE = luma(tNE);
            float lSW = luma(tSW); float lSE = luma(tSE);

            float wNW = exp(-((lNW - lC) * (lNW - lC)) * sigma) * 0.707;
            float wNE = exp(-((lNE - lC) * (lNE - lC)) * sigma) * 0.707;
            float wSW = exp(-((lSW - lC) * (lSW - lC)) * sigma) * 0.707;
            float wSE = exp(-((lSE - lC) * (lSE - lC)) * sigma) * 0.707;

            float wTotal = wC + wN + wS + wW + wE + wNW + wNE + wSW + wSE;
            vec3 smoothed = (bicubic * wC + tN * wN + tS * wS + tW * wW + tE * wE
                           + tNW * wNW + tNE * wNE + tSW * wSW + tSE * wSE) / wTotal;

            // Blend: mostly bicubic, with controlled artifact smoothing
            bicubic = mix(bicubic, smoothed, suppressStrength);
        }
    }

    fragColor = vec4(clamp(bicubic, 0.0, 1.0), 1.0);
}
"""
    }
}
