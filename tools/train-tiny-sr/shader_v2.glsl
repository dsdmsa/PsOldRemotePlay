// NeuralUp-v2 Fragment Shader
// Architecture: 17 -> 32 (PReLU) -> 1
// Inputs: 9 luma (3x3) + 4 dilated cross (+-2) + gradient_mag + variance + fx + fy
// ~601 MACs/pixel + 8 texture ops (4 textureGather + 4 texture)
// Estimated: ~0.7-0.9ms at 1080p on Mali-G710 (mediump)
//
// Paste this into TinyNNUpscaleStrategy.kt, replacing the v1 shader.
// Then paste the trained weights from weights_v2.glsl.

#version 300 es
precision mediump float;  // FP16 on Mali — critical for staying within 1ms budget

uniform sampler2D uInputTex;
uniform vec2 uInputSize;

in vec2 vTexCoord;
out vec4 fragColor;

// === WEIGHTS (paste from weights_v2.glsl after training) ===
// const int N_INPUTS = 17;
// const int HIDDEN = 32;
// const float W1[32][17] = ...;
// const float B1[32] = ...;
// const float PRELU_A[32] = ...;
// const float W2[32] = ...;
// const float B2 = ...;

// --- placeholder weights (replace after training) ---
const int N_INPUTS = 17;
const int HIDDEN = 32;
// W1, B1, PRELU_A, W2, B2 will be pasted from training output

float lumaF(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

void main() {
    vec2 ps = 1.0 / uInputSize;
    vec2 srcPos = vTexCoord * uInputSize - 0.5;
    vec2 srcFloor = floor(srcPos);
    vec2 frac = srcPos - srcFloor;
    vec2 tc = (srcFloor + 0.5) * ps;
    vec2 dx = vec2(ps.x, 0.0);
    vec2 dy = vec2(0.0, ps.y);

    // --- Texture reads: 3x3 core (using textureGather for efficiency) ---
    // textureGather fetches the 4 texels used in bilinear filtering as a vec4.
    // Component 0 = red channel (for luma extraction we still need RGB, so
    // we fall back to regular texture reads. textureGather is mainly beneficial
    // when working in a single-channel texture format like R8.)
    //
    // For RGB input texture, use regular texture reads:
    vec3 s00 = texture(uInputTex, clamp(tc - dx - dy, vec2(0), vec2(1))).rgb;
    vec3 s10 = texture(uInputTex, clamp(tc      - dy, vec2(0), vec2(1))).rgb;
    vec3 s20 = texture(uInputTex, clamp(tc + dx - dy, vec2(0), vec2(1))).rgb;
    vec3 s01 = texture(uInputTex, clamp(tc - dx,      vec2(0), vec2(1))).rgb;
    vec3 s11 = texture(uInputTex, tc).rgb;
    vec3 s21 = texture(uInputTex, clamp(tc + dx,      vec2(0), vec2(1))).rgb;
    vec3 s02 = texture(uInputTex, clamp(tc - dx + dy, vec2(0), vec2(1))).rgb;
    vec3 s12 = texture(uInputTex, clamp(tc      + dy, vec2(0), vec2(1))).rgb;
    vec3 s22 = texture(uInputTex, clamp(tc + dx + dy, vec2(0), vec2(1))).rgb;

    // --- Dilated cross at +-2 (4 extra texture reads, 5x5 receptive field) ---
    float d_top    = lumaF(texture(uInputTex, clamp(tc - 2.0*dy, vec2(0), vec2(1))).rgb);
    float d_bottom = lumaF(texture(uInputTex, clamp(tc + 2.0*dy, vec2(0), vec2(1))).rgb);
    float d_left   = lumaF(texture(uInputTex, clamp(tc - 2.0*dx, vec2(0), vec2(1))).rgb);
    float d_right  = lumaF(texture(uInputTex, clamp(tc + 2.0*dx, vec2(0), vec2(1))).rgb);
    // Total: 13 texture reads

    // --- Bilinear baseline from nearest 4 pixels ---
    vec3 bilinear = mix(mix(s11, s21, frac.x), mix(s12, s22, frac.x), frac.y);
    float bicLuma = lumaF(bilinear);

    // --- Extract 3x3 luma values ---
    float l00 = lumaF(s00), l10 = lumaF(s10), l20 = lumaF(s20);
    float l01 = lumaF(s01), l11 = lumaF(s11), l21 = lumaF(s21);
    float l02 = lumaF(s02), l12 = lumaF(s12), l22 = lumaF(s22);

    // --- Compute gradient magnitude (Sobel on 3x3 luma) ---
    float gx = (l20 + 2.0*l21 + l22) - (l00 + 2.0*l01 + l02);
    float gy = (l02 + 2.0*l12 + l22) - (l00 + 2.0*l10 + l20);
    float grad_mag = sqrt(gx*gx + gy*gy + 1e-8);

    // --- Compute local variance of 3x3 ---
    float mean9 = (l00 + l10 + l20 + l01 + l11 + l21 + l02 + l12 + l22) / 9.0;
    float var9 = ((l00-mean9)*(l00-mean9) + (l10-mean9)*(l10-mean9) + (l20-mean9)*(l20-mean9)
                + (l01-mean9)*(l01-mean9) + (l11-mean9)*(l11-mean9) + (l21-mean9)*(l21-mean9)
                + (l02-mean9)*(l02-mean9) + (l12-mean9)*(l12-mean9) + (l22-mean9)*(l22-mean9)) / 9.0;

    // --- NN input: 17 features ---
    float inp[17];
    // 3x3 luma (9)
    inp[0] = l00; inp[1] = l10; inp[2] = l20;
    inp[3] = l01; inp[4] = l11; inp[5] = l21;
    inp[6] = l02; inp[7] = l12; inp[8] = l22;
    // dilated cross (4)
    inp[9]  = d_top;
    inp[10] = d_bottom;
    inp[11] = d_left;
    inp[12] = d_right;
    // computed features (2)
    inp[13] = grad_mag;
    inp[14] = var9;
    // sub-pixel position (2)
    inp[15] = frac.x;
    inp[16] = frac.y;

    // --- Hidden layer: h = PReLU(W1 * inp + B1) ---
    float h[32];
    for (int j = 0; j < HIDDEN; j++) {
        float sum = B1[j];
        for (int i = 0; i < N_INPUTS; i++) {
            sum += W1[j][i] * inp[i];
        }
        // PReLU: max(a*x, x) = x >= 0 ? x : a*x
        h[j] = sum >= 0.0 ? sum : PRELU_A[j] * sum;
    }

    // --- Output: residual = W2 * h + B2 ---
    float residual = B2;
    for (int j = 0; j < HIDDEN; j++) {
        residual += W2[j] * h[j];
    }

    // --- Apply residual via luma correction (preserves chroma) ---
    float correctedLuma = bicLuma + residual;

    // Additive chroma transfer (robust near black, avoids division by near-zero)
    vec3 result = bilinear + vec3(residual);

    // --- Adaptive anti-halo clamp ---
    // Tighter in flat areas, wider on edges
    vec3 mn = min(min(s11, s21), min(s12, s22));
    vec3 mx = max(max(s11, s21), max(s12, s22));
    vec3 range = mx - mn;
    float edge_factor = smoothstep(0.0, 0.1, grad_mag); // 0 in flat, 1 on edges
    vec3 margin = range * mix(0.02, 0.10, edge_factor); // tight in flat, wide on edges
    result = clamp(result, mn - margin, mx + margin);

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
