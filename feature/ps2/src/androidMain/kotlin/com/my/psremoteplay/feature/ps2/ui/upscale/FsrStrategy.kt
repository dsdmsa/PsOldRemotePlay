package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * AMD FidelityFX Super Resolution 1.0 — corrected EASU + RCAS with noise gate.
 *
 * 2-pass strategy:
 *   Pass 1 (EASU): Edge-Adaptive Spatial Upscaling — 12-tap directional Lanczos
 *                   with kernel rotation, proper sub-pixel mapping, and deringing clamp.
 *   Pass 2 (RCAS): Robust Contrast-Adaptive Sharpening — 5-tap cross filter with
 *                   noise detection to avoid amplifying compression artifacts.
 */
class FsrStrategy : UpscaleStrategy {

    override val name: String = "FSR 1.0 (Fixed)"

    override val isSinglePass: Boolean = false

    override val upscaleFragShader: String
        get() = EASU_FRAG.trimIndent().trim()

    override val sharpenFragShader: String
        get() = RCAS_FRAG.trimIndent().trim()

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
        private const val EASU_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;
uniform vec2 uOutputSize;

in vec2 vTexCoord;
out vec4 fragColor;

// Rec. 709 luma
float FsrLuma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// AMD's Lanczos2 approximation (BOTH components):
// (25/16 * (2/5*d2 - 1)^2 - (25/16-1)) * (1/4*d2 - 1)^2
float FsrEasuLanczos(float d2) {
    // Window component: (1/4 * d2 - 1)^2
    float wA = 0.25 * d2 - 1.0;
    wA *= wA;
    // Base component: 25/16 * (2/5 * d2 - 1)^2 - (25/16-1)
    float wB = 0.4 * d2 - 1.0;
    wB = 1.5625 * wB * wB - 0.5625;
    return wB * wA;
}

// Accumulate one tap: rotate offset by edge direction, apply anisotropic scaling
void FsrEasuTap(
    inout vec3 aC, inout float aW,
    vec2 off,       // tap offset from sub-pixel position
    vec2 dir,       // normalized edge direction
    vec2 len,       // anisotropic scale factors
    float lob,      // negative lobe clamp
    vec3 c          // tap color
) {
    // Rotate offset by edge direction
    vec2 v;
    v.x = off.x * dir.x + off.y * dir.y;
    v.y = off.x * (-dir.y) + off.y * dir.x;
    // Apply anisotropic scaling
    v *= len;
    // Squared distance
    float d2 = v.x * v.x + v.y * v.y;
    // Clamp to limit support window
    d2 = min(d2, lob);
    // Compute weight
    float w = FsrEasuLanczos(d2);
    aC += c * w;
    aW += w;
}

// Bilinear region edge analysis (one of 4 sub-pixel quadrants)
void FsrEasuSet(
    inout vec2 dir, inout float len,
    float w,        // bilinear weight for this quadrant
    float lA, float lB, float lC, float lD, float lE
) {
    // Horizontal edge detection
    float dc = lD - lC;
    float cb = lC - lB;
    float lenX = max(abs(dc), abs(cb));
    lenX = 1.0 / (lenX + 1e-5);  // approximate reciprocal
    float dirX = lD - lB;
    dir.x += dirX * w;
    lenX = clamp(abs(dirX) * lenX, 0.0, 1.0);
    lenX *= lenX;
    len += lenX * w;

    // Vertical edge detection
    float ec = lE - lC;
    float ca = lC - lA;
    float lenY = max(abs(ec), abs(ca));
    lenY = 1.0 / (lenY + 1e-5);
    float dirY = lE - lA;
    dir.y += dirY * w;
    lenY = clamp(abs(dirY) * lenY, 0.0, 1.0);
    lenY *= lenY;
    len += lenY * w;
}

void main() {
    vec2 ps = 1.0 / uInputSize;  // pixel size in texcoord space

    // Map output pixel to input position
    vec2 srcPos = vTexCoord * uInputSize - 0.5;
    vec2 srcFp = floor(srcPos);
    vec2 pp = srcPos - srcFp;  // sub-pixel position [0,1)

    // Center of the nearest input pixel
    vec2 tc = (srcFp + 0.5) * ps;
    vec2 dx = vec2(ps.x, 0.0);
    vec2 dy = vec2(0.0, ps.y);

    // 12-tap sampling pattern:
    //    b c
    //  e f g h
    //  i j k l
    //    n o
    vec3 b = texture(uInputTex, tc - dy).rgb;
    vec3 c = texture(uInputTex, tc - dy + dx).rgb;
    vec3 e = texture(uInputTex, tc - dx).rgb;
    vec3 f = texture(uInputTex, tc).rgb;
    vec3 g = texture(uInputTex, tc + dx).rgb;
    vec3 h = texture(uInputTex, tc + 2.0 * dx).rgb;
    vec3 i = texture(uInputTex, tc + dy - dx).rgb;
    vec3 j = texture(uInputTex, tc + dy).rgb;
    vec3 k = texture(uInputTex, tc + dy + dx).rgb;
    vec3 l = texture(uInputTex, tc + dy + 2.0 * dx).rgb;
    vec3 n = texture(uInputTex, tc + 2.0 * dy).rgb;
    vec3 o = texture(uInputTex, tc + 2.0 * dy + dx).rgb;

    // Luma for edge detection
    float bL = FsrLuma(b), cL = FsrLuma(c);
    float eL = FsrLuma(e), fL = FsrLuma(f), gL = FsrLuma(g), hL = FsrLuma(h);
    float iL = FsrLuma(i), jL = FsrLuma(j), kL = FsrLuma(k), lL = FsrLuma(l);
    float nL = FsrLuma(n), oL = FsrLuma(o);

    // === Edge direction detection via 4 bilinear regions ===
    // Each region covers a 2x3 or 3x2 area centered on a bilinear quadrant
    vec2 dir = vec2(0.0);
    float len = 0.0;

    // Bilinear weights for sub-pixel position
    float w0 = (1.0 - pp.x) * (1.0 - pp.y);  // top-left
    float w1 = pp.x * (1.0 - pp.y);            // top-right
    float w2 = (1.0 - pp.x) * pp.y;            // bottom-left
    float w3 = pp.x * pp.y;                     // bottom-right

    //        lA
    //     lB lC lD
    //        lE
    FsrEasuSet(dir, len, w0, bL, eL, fL, gL, jL);  // around f
    FsrEasuSet(dir, len, w1, cL, fL, gL, hL, kL);  // around g
    FsrEasuSet(dir, len, w2, fL, iL, jL, kL, nL);  // around j
    FsrEasuSet(dir, len, w3, gL, jL, kL, lL, oL);  // around k

    // Normalize direction
    float dirLen = dir.x * dir.x + dir.y * dir.y;
    float dirInvLen = inversesqrt(max(dirLen, 1e-8));
    dir *= dirInvLen;

    // Compute anisotropic stretch from direction magnitude
    // stretch = (x^2+y^2) / max(|x|,|y|)  -- maps [1, sqrt(2)]
    float stretch = (dir.x * dir.x + dir.y * dir.y) /
                    max(abs(dir.x), abs(dir.y));

    // Anisotropic filter kernel size
    // len2.x controls stretch along edge, len2.y across edge
    vec2 len2;
    len2.x = 1.0 + (stretch - 1.0) * len;
    len2.y = 1.0 + (-0.5) * len;

    // Negative lobe strength: softer for sharp edges, harder for soft
    float lob = 0.5 + (0.25 - 0.04 - 0.5) * len;
    // Clamp distance for kernel support window
    float clp = 1.0 / lob;

    // === Deringing: compute 2x2 neighborhood min/max ===
    vec3 min4 = min(min(f, g), min(j, k));
    vec3 max4 = max(max(f, g), max(j, k));

    // === Accumulate 12 taps with rotated/stretched kernel ===
    vec3 aC = vec3(0.0);
    float aW = 0.0;

    // Offsets relative to sub-pixel position pp
    FsrEasuTap(aC, aW, vec2( 0.0,-1.0) - pp, dir, len2, clp, b);
    FsrEasuTap(aC, aW, vec2( 1.0,-1.0) - pp, dir, len2, clp, c);
    FsrEasuTap(aC, aW, vec2(-1.0, 0.0) - pp, dir, len2, clp, e);
    FsrEasuTap(aC, aW, vec2( 0.0, 0.0) - pp, dir, len2, clp, f);
    FsrEasuTap(aC, aW, vec2( 1.0, 0.0) - pp, dir, len2, clp, g);
    FsrEasuTap(aC, aW, vec2( 2.0, 0.0) - pp, dir, len2, clp, h);
    FsrEasuTap(aC, aW, vec2(-1.0, 1.0) - pp, dir, len2, clp, i);
    FsrEasuTap(aC, aW, vec2( 0.0, 1.0) - pp, dir, len2, clp, j);
    FsrEasuTap(aC, aW, vec2( 1.0, 1.0) - pp, dir, len2, clp, k);
    FsrEasuTap(aC, aW, vec2( 2.0, 1.0) - pp, dir, len2, clp, l);
    FsrEasuTap(aC, aW, vec2( 0.0, 2.0) - pp, dir, len2, clp, n);
    FsrEasuTap(aC, aW, vec2( 1.0, 2.0) - pp, dir, len2, clp, o);

    // Normalize and apply deringing clamp
    vec3 result = aC / max(aW, 1e-5);
    result = clamp(result, min4, max4);

    fragColor = vec4(result, 1.0);
}
"""

        private const val RCAS_FRAG = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uTexelSize;
uniform float uSharpness;  // 0.0 = max sharpen, 1.0 = none

in vec2 vTexCoord;
out vec4 fragColor;

void main() {
    // 5-tap cross pattern
    vec3 e = texture(uInputTex, vTexCoord).rgb;
    vec3 b = texture(uInputTex, vTexCoord + vec2(0.0, -uTexelSize.y)).rgb;
    vec3 d = texture(uInputTex, vTexCoord + vec2(-uTexelSize.x, 0.0)).rgb;
    vec3 f = texture(uInputTex, vTexCoord + vec2( uTexelSize.x, 0.0)).rgb;
    vec3 h = texture(uInputTex, vTexCoord + vec2(0.0,  uTexelSize.y)).rgb;

    // Work in luma space for weight calculation
    float bL = dot(b, vec3(0.2126, 0.7152, 0.0722));
    float dL = dot(d, vec3(0.2126, 0.7152, 0.0722));
    float eL = dot(e, vec3(0.2126, 0.7152, 0.0722));
    float fL = dot(f, vec3(0.2126, 0.7152, 0.0722));
    float hL = dot(h, vec3(0.2126, 0.7152, 0.0722));

    // Min/max of cross neighbors
    float mn = min(min(bL, dL), min(fL, hL));
    float mx = max(max(bL, dL), max(fL, hL));
    mn = min(mn, eL);
    mx = max(mx, eL);

    // Noise detection: highpass filter normalized by local contrast
    // This prevents sharpening of compression artifacts
    float nz = 0.25 * (bL + dL + fL + hL) - eL;
    nz = clamp(abs(nz) / (mx - mn + 1e-5), 0.0, 1.0);
    nz = 1.0 - 0.5 * nz;  // 1.0 = clean, 0.5 = noisy

    // Solve for maximum sharpening that doesn't clip
    // hitMin: how much can we go below without exceeding min?
    // hitMax: how much can we go above without exceeding max?
    float hitMin = mn / (4.0 * mx + 1e-5);
    float hitMax = (1.0 - mx) / (4.0 * mn - 4.0 + 1e-5);

    float w = max(-hitMin, hitMax);
    w = clamp(w, -0.1875, 0.0);  // FSR_RCAS_LIMIT = 0.25 - 1/16
    w *= mix(1.0, nz, 1.0);      // Apply noise suppression
    w *= (1.0 - uSharpness);     // User sharpness control

    // Apply weighted cross filter
    float rcpW = 1.0 / (1.0 + 4.0 * w);
    vec3 result = clamp((e + w * (b + d + f + h)) * rcpW, 0.0, 1.0);

    fragColor = vec4(result, 1.0);
}
"""
    }
}
