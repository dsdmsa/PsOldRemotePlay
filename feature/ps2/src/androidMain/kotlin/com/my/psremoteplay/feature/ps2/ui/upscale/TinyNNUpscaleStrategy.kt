package com.my.psremoteplay.feature.ps2.ui.upscale

import android.opengl.GLES30

/**
 * Tiny Neural Network Upscaler — 209 trained parameters.
 *
 * Architecture: 11 inputs (9 luma + 2 sub-pixel) → 16 hidden (ReLU) → 1 luma residual
 * Trained on PS2 game screenshots with H.264 compression simulation.
 * Adds a learned correction on top of bilinear interpolation.
 *
 * Training result: +0.42dB PSNR over bilinear baseline.
 * GPU cost: ~192 MACs/pixel + 9 texture reads = ~0.4ms at 1080p on Mali-G710.
 */
class TinyNNUpscaleStrategy : UpscaleStrategy {
    override val name = "Tiny NN"
    override val isSinglePass = true
    override val upscaleFragShader = SHADER.trimIndent().trim()

    override fun setUpscaleUniforms(program: Int, inputWidth: Int, inputHeight: Int, outputWidth: Int, outputHeight: Int) {
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "uInputTex"), 0)
        GLES30.glUniform2f(GLES30.glGetUniformLocation(program, "uInputSize"), inputWidth.toFloat(), inputHeight.toFloat())
    }

    companion object {
        private const val SHADER = """
#version 300 es
precision highp float;

uniform sampler2D uInputTex;
uniform vec2 uInputSize;

in vec2 vTexCoord;
out vec4 fragColor;

// Trained weights: 11 -> 16 (ReLU) -> 1, 209 params
// Trained on PS2 Batman Begins screenshots with JPEG compression simulation
const int HIDDEN = 16;

const float W1[16][11] = float[16][11](
    float[11](-0.16855514, -0.92925972, -0.98442978, 0.34329975, 0.41149089, 0.22072943, -0.04198847, 0.37030515, -0.06592251, 0.04220565, -0.51154804),
    float[11](-0.07172213, 0.18855977, -0.23843971, -0.23946273, -0.43159166, -0.47970226, 0.37244648, 0.36405545, 0.56479651, 0.00686237, -0.55412102),
    float[11](-0.78766543, 0.27380177, 0.06921509, -1.17025030, 0.21329223, 0.10231556, -0.51428944, 0.62975210, -0.17388032, -1.24921358, -0.00312707),
    float[11](0.63439149, 0.11995757, -0.08222373, 0.47934717, -0.45203638, -1.10146153, 0.36486289, -0.31616867, -0.20023936, -0.01690165, -0.04872619),
    float[11](-0.01767764, -0.00788193, -0.35041004, -0.09448076, 0.96657574, 0.03870355, -1.34436381, -0.04397606, -0.16506277, 0.08820263, -0.69907105),
    float[11](-0.30537009, -0.79603571, -0.81097186, 0.22148766, 0.87891078, 0.31425083, -0.01122448, 0.27848166, -0.26841092, -0.01401283, 0.71582770),
    float[11](-0.83545327, 0.50713247, -0.16818498, -0.38301194, 0.20186435, 0.47672352, -0.23967513, 0.44186223, -0.37370232, 0.20269307, 0.03770854),
    float[11](0.59170586, -0.03963555, -0.16520661, -0.53812784, -0.86841118, 0.12053194, 0.35098445, -0.05003193, 0.51457310, 0.00418267, 0.28231433),
    float[11](0.15099388, -0.21561404, 0.27309108, 0.04560194, 0.14129822, 0.68960351, 0.67553627, -0.19050325, -0.41252032, 0.75457972, -0.44232202),
    float[11](0.02645458, 0.07660850, 0.02288682, 0.92380655, -0.50648451, 0.48970509, -0.46826959, -1.28756344, -0.40403846, -0.00188941, 0.35511214),
    float[11](-0.27587792, 0.43114656, 0.44837236, 0.66333401, 0.67023766, -0.62157011, -0.26402062, -0.54503191, -1.01385558, -0.06441662, -0.73462152),
    float[11](0.14532357, -0.68342513, -0.21842854, 0.04673654, 0.06423801, 0.48259154, 0.34043232, -0.41981754, 0.13767043, 0.00414749, 0.80593520),
    float[11](0.89484823, -0.62952465, -0.29825518, -1.46706676, -0.01741776, -0.13710219, -0.03300381, 0.17092586, 0.50379497, 0.00292463, 0.52349991),
    float[11](-0.08584571, 0.34847462, 0.75314569, -0.18189706, -1.06106544, 0.02816024, 0.30322102, -0.26242957, 0.18261577, -0.01308800, 0.13431822),
    float[11](0.42106748, 0.31728929, -0.73259002, 0.47889698, 0.29152438, -0.73200393, -0.28528440, 0.52131164, -0.58537388, -0.91888881, 0.04546573),
    float[11](-0.69341570, -0.00391920, 0.07384969, 0.95475972, 0.55622774, 0.27579132, -0.29096568, -0.17103378, -0.45623872, 0.13464345, 0.22846797)
);

const float B1[16] = float[16](-0.03807065, 0.06361828, 0.20749816, 0.01309223, 0.03799388, -0.61370653, -0.20911601, -0.10336015, 0.16259143, -0.29892609, 0.06629603, -0.21222802, -0.43988922, -0.12412796, 0.05428797, -0.00457107);

const float W2[16] = float[16](-0.13392687, -0.11094674, -0.45129395, -0.07866702, 0.39380315, 0.22080863, 0.19551767, 0.08251771, -0.01240126, -0.16858803, 0.13422555, -0.05422284, -0.25943333, -0.10629159, 0.15783808, 0.06817199);

const float B2 = -0.00125137;

float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

void main() {
    vec2 ps = 1.0 / uInputSize;
    vec2 srcPos = vTexCoord * uInputSize - 0.5;
    vec2 srcFloor = floor(srcPos);
    vec2 frac = srcPos - srcFloor;
    vec2 tc = (srcFloor + 0.5) * ps;
    vec2 dx = vec2(ps.x, 0.0);
    vec2 dy = vec2(0.0, ps.y);

    // 3x3 neighborhood
    vec3 s00 = texture(uInputTex, clamp(tc - dx - dy, vec2(0), vec2(1))).rgb;
    vec3 s10 = texture(uInputTex, clamp(tc      - dy, vec2(0), vec2(1))).rgb;
    vec3 s20 = texture(uInputTex, clamp(tc + dx - dy, vec2(0), vec2(1))).rgb;
    vec3 s01 = texture(uInputTex, clamp(tc - dx,      vec2(0), vec2(1))).rgb;
    vec3 s11 = texture(uInputTex, tc).rgb;
    vec3 s21 = texture(uInputTex, clamp(tc + dx,      vec2(0), vec2(1))).rgb;
    vec3 s02 = texture(uInputTex, clamp(tc - dx + dy, vec2(0), vec2(1))).rgb;
    vec3 s12 = texture(uInputTex, clamp(tc      + dy, vec2(0), vec2(1))).rgb;
    vec3 s22 = texture(uInputTex, clamp(tc + dx + dy, vec2(0), vec2(1))).rgb;

    // Bilinear baseline from nearest 4 pixels
    vec3 bilinear = mix(mix(s11, s21, frac.x), mix(s12, s22, frac.x), frac.y);
    float bicLuma = luma(bilinear);

    // NN input: 9 luma + 2 fractional position
    float inp[11];
    inp[0] = luma(s00); inp[1] = luma(s10); inp[2] = luma(s20);
    inp[3] = luma(s01); inp[4] = luma(s11); inp[5] = luma(s21);
    inp[6] = luma(s02); inp[7] = luma(s12); inp[8] = luma(s22);
    inp[9] = frac.x; inp[10] = frac.y;

    // Hidden layer: h = ReLU(W1 * inp + B1)
    float h[16];
    for (int j = 0; j < HIDDEN; j++) {
        float sum = B1[j];
        for (int i = 0; i < 11; i++) sum += W1[j][i] * inp[i];
        h[j] = max(0.0, sum);
    }

    // Output: residual = W2 * h + B2
    float residual = B2;
    for (int j = 0; j < HIDDEN; j++) residual += W2[j] * h[j];

    // Apply residual via luma ratio (preserves chroma)
    float correctedLuma = bicLuma + residual;
    float ratio = (bicLuma > 0.001) ? clamp(correctedLuma / bicLuma, 0.8, 1.2) : 1.0;
    vec3 result = bilinear * ratio;

    // Anti-halo clamp
    vec3 mn = min(min(s11, s21), min(s12, s22));
    vec3 mx = max(max(s11, s21), max(s12, s22));
    vec3 margin = (mx - mn) * 0.05;
    result = clamp(result, mn - margin, mx + margin);

    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
"""
    }
}
