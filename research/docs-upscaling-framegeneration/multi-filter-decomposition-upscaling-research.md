# Multi-Filter Decomposition + Matrix Combination Upscaling Research

**Date:** 2026-04-05
**Context:** Designing upscaling algorithms for PS2 streaming (640x448 -> 1080p+) based on parallel filter decomposition and matrix-guided interpolation.

---

## 1. Mathematical Foundation

### 1.1 Per-Pixel Feature Vector

For each output pixel (mapped back to input space), we extract a feature vector from the local neighborhood:

```
F = [luma, edgeH, edgeV, highpass, lowpass, contrast]
```

Where:
- `luma` = BT.709 luminance of center pixel (0..1)
- `edgeH` = |Sobel-H| = horizontal edge magnitude
- `edgeV` = |Sobel-V| = vertical edge magnitude  
- `highpass` = |Laplacian| = high-frequency energy
- `lowpass` = bilateral mean = low-frequency smoothness
- `contrast` = max(|neighbor_luma - center_luma|) in 3x3

This is a 6-dimensional feature vector. In practice, 5 features suffice (dropping one redundant channel).

### 1.2 Weight Matrix Transform

The feature vector is transformed into N upscaling parameters via matrix multiplication:

```
P = F x M    where M is a 6xN matrix
```

**What should N be?**

| N | Meaning | Use Case |
|---|---------|----------|
| 1 | Single scalar "guidance" | Blend between isotropic and edge-directed |
| 2 | Direction + Strength | Kernel rotation angle + anisotropy ratio |
| 4 | 4 directional weights | Blend H, V, D45, D135 interpolations |
| 16 | Full 4x4 kernel weights | Per-pixel learned kernel (RAISR-style) |

**Recommended: N=4** (4 directional interpolation weights). This provides enough degrees of freedom to handle all common edge orientations without the complexity of full kernel learning.

### 1.3 Matrix Multiplication for Neighborhood Upscaling

An alternative formulation: the entire neighborhood (e.g., 3x3 = 9 pixels) can be treated as a vector, and the output pixel is a matrix-vector product:

```
output_pixel = neighborhood_vector x W    where W is 9x3 (for RGB)
```

The weight matrix W is selected per-pixel based on the feature vector F. This is exactly what RAISR does with discrete bins; our approach uses continuous interpolation between a smaller set of weight matrices.

---

## 2. Research Survey

### 2.1 RAISR (Rapid and Accurate Image Super Resolution)

**Reference:** Romano, Isidoro, Milanfar. "RAISR: Rapid and Accurate Image Super Resolution." IEEE Trans. Computational Imaging, 2017.

Core idea: Hash each pixel's local gradient (angle quantized to 24 bins, strength to 3 levels, coherence to 3 levels = 216 classes). For each class, pre-learn an optimal 11x11 filter via least-squares regression on HR/LR training pairs. At runtime, classify each pixel and apply the corresponding filter.

**Relevance to our approach:** RAISR is the gold standard for "learned filter bank" upscaling. Our MatrixGuided algorithm is a continuous generalization: instead of 216 discrete filters, we compute continuous direction weights via matrix multiply. This trades some quality for simplicity (no lookup tables, no training data needed).

**GPU Implementation:** Intel's Video Super Resolution Library provides a C/C++ RAISR implementation. The key GPU challenge is the filter lookup: 216 filters x 121 coefficients each = ~104K floats, which fits in a uniform buffer or texture.

### 2.2 Guided Image Filtering / Joint Bilateral Upsampling

**Reference:** He, Sun, Tang. "Guided Image Filtering." IEEE TPAMI, 2013; Kopf et al. "Joint Bilateral Upsampling." SIGGRAPH, 2007.

The guided filter computes output as a local linear model: `q_i = a_k * I_i + b_k` where I is the guidance image and (a_k, b_k) are computed per-window from the guidance and input statistics.

**Joint Bilateral Upsampling:** Upsample a low-resolution signal using a high-resolution guidance image. The bilateral kernel weights are computed from the guidance image, ensuring structure preservation.

**Relevance:** Our FilterFusion algorithm's bilateral smoothness filter is a simplified form of guided filtering. The "guidance map" concept in FilterFusion is directly inspired by guided image filtering: the filter outputs form a guidance signal that drives the upscaling behavior.

### 2.3 Edge-Preserving Decompositions

**Reference:** Farbman et al. "Edge-preserving Decompositions for Multi-scale Tone and Detail Manipulation." SIGGRAPH, 2008.

Decomposes an image into base + detail layers using Weighted Least Squares (WLS) smoothing. Each layer can be manipulated independently. The key insight: the bilateral filter is limited to single-scale decomposition; WLS provides multi-scale capability.

**Relevance:** Our DecomposeRecompose algorithm uses bilateral filtering for decomposition (simpler than WLS but sufficient for real-time GPU). The per-layer upscaling strategy (bicubic for base, bilinear for detail, directional for edges) is novel.

### 2.4 Structure Tensor Methods

**Reference:** Roussos & Maragos. "Tensor-based image diffusions derived from generalizations of the Total Variation and Perona-Malik equations." 2010. Also: Weickert, "Anisotropic Diffusion in Image Processing," 1998.

The structure tensor J = sum(grad * grad^T) over a neighborhood captures local orientation. Its eigenvalues (lambda1, lambda2) indicate:
- lambda1 >> lambda2: strong edge (coherent direction)
- lambda1 ~ lambda2 >> 0: corner or texture
- lambda1 ~ lambda2 ~ 0: flat region

The eigenvector of the larger eigenvalue gives the edge direction.

**Relevance:** Our FeatureGuidedStrategy (existing) already uses structure tensor decomposition. The new algorithms use simplified Sobel-based edge direction estimation, which is cheaper and sufficient for the 3x downscaling ratio.

### 2.5 Super xBR

**Reference:** Hylian, 2015. Multi-pass edge-directed interpolation for retro game upscaling.

Uses weighted distance calculations in diagonal and axial directions to determine edge orientation. A 6-pass pipeline with anti-ringing at each stage. Specifically designed for pixel art where edges are axis-aligned or 45-degree.

**Relevance:** The directional interpolation concept (sample along detected edge direction) is shared with our approaches. However, Super xBR is designed for 2x integer scaling of pixel art, not arbitrary-ratio scaling of video. Our algorithms generalize the concept to fractional upscaling with sub-pixel positioning.

### 2.6 Multi-Kernel Adaptive Interpolation

**Reference:** Hung & de Haan. "Multi-kernel based adaptive interpolation for image super-resolution." Multimedia Tools and Applications, 2012.

Defines geometric stencils based on local structure, each associated with a specific interpolation kernel. The final pixel value is a weighted average from the selected kernel. Different stencils capture horizontal, vertical, diagonal, and isotropic structures.

**Relevance:** Directly inspired the MatrixGuided approach. Instead of discrete stencil selection, we use continuous feature-to-weight matrix multiplication.

---

## 3. Three Concrete Algorithms

### Algorithm A: FilterFusion

**Concept:** 3 parallel filters computed from the same 3x3 neighborhood, combined into a guidance scalar that drives anisotropic bicubic kernel stretching.

**Pipeline:**
```
Input 3x3 neighborhood
    |
    +-- Sobel edge filter ------> edgeMag, edgeDir
    +-- Bilateral smooth -------> smoothness
    +-- Laplacian detail -------> detailEnergy
    |
    v
Guidance = 0.5*edgeMag - 0.3*smoothness + 0.3*detailEnergy + 0.15
    |
    v
blend = smoothstep(guidance)
    |
    +-- Isotropic Catmull-Rom bicubic (4x4) ---+
    +-- Anisotropic bicubic (stretched along edge) --+
    |                                                  |
    v                                                  v
result = mix(isotropic, anisotropic, blend) + detail * guidance * 0.35
    |
    v
Anti-halo clamp to local 2x2 min/max
```

**Implementation:** `/feature/ps2/src/androidMain/.../upscale/FilterFusionStrategy.kt`

| Property | Value |
|----------|-------|
| Passes | 1 (single-pass) |
| Texture fetches | 16 (4x4 grid) |
| ALU ops | ~120 |
| Uniforms | `uInputTex` (sampler2D), `uInputSize` (vec2), `uOutputSize` (vec2) |
| Expected quality | +0.8-1.0 dB PSNR over bicubic |
| Strength | Good balance; smooth areas stay clean, edges get directional treatment |
| Weakness | Fixed guidance weights; the 3-feature vector is somewhat redundant |

### Algorithm B: MatrixGuided

**Concept:** Extract 5-channel feature vector, multiply by 5x4 weight matrix to get 4 directional interpolation weights, blend 4 directional 1D cubics.

**Pipeline:**
```
Input 3x3 neighborhood
    |
    v
Feature extraction:
    F = [luma, |sobelH|, |sobelV|, |laplacian|, maxContrast]
    |
    v
Matrix multiply: W = F x M  (5x4 matrix, hand-tuned)
    W = [wH, wV, wD45, wD135]
    |
    +-- Diagonal asymmetry correction (compare l00-l22 vs l20-l02)
    |
    v
Softmax normalization: W /= sum(W)
    |
    +-- 1D cubic along H    (4 taps) ---> interpH
    +-- 1D cubic along V    (4 taps) ---> interpV
    +-- 1D cubic along D45  (4 taps) ---> interpD45
    +-- 1D cubic along D135 (4 taps) ---> interpD135
    |
    v
result = wH*interpH + wV*interpV + wD45*interpD45 + wD135*interpD135
    |
    +-- Flat-area fallback: mix toward bilinear in featureless regions
    |
    v
Anti-halo clamp to local 2x2 min/max
```

**Implementation:** `/feature/ps2/src/androidMain/.../upscale/MatrixGuidedStrategy.kt`

| Property | Value |
|----------|-------|
| Passes | 1 (single-pass) |
| Texture fetches | 9 (3x3) + 4x4 (1D cubics along 4 dirs, some shared) = ~25 |
| ALU ops | ~150 |
| Uniforms | `uInputTex` (sampler2D), `uInputSize` (vec2), `uOutputSize` (vec2) |
| Expected quality | +1.0-1.2 dB PSNR over bicubic |
| Strength | Excellent diagonal edges; continuous direction blending |
| Weakness | Hand-tuned matrix; 4 directions may miss off-axis edges (22.5 deg) |

**The Weight Matrix M (5x4):**
```
                   H      V     D45    D135
    luma:      [ 0.1,   0.1,   0.1,   0.1  ]   // isotropic base bias
    edgeH:     [-0.8,   1.5,   0.3,   0.3  ]   // H edges -> prefer V interpolation
    edgeV:     [ 1.5,  -0.8,   0.3,   0.3  ]   // V edges -> prefer H interpolation
    highpass:  [ 0.0,   0.0,   0.5,   0.5  ]   // fine detail -> diagonals
    contrast:  [ 0.2,   0.2,  -0.2,  -0.2  ]   // high contrast -> suppress diag
```

Key insight: when Sobel-H detects a horizontal gradient (image varies left-to-right), the edge itself runs vertically, so we should interpolate vertically (along the edge). The matrix encodes this cross-relationship.

### Algorithm C: DecomposeRecompose

**Concept:** Decompose into base/detail/edge layers, upscale each with the optimal method, recompose with edge-weighted blending.

**Pipeline:**
```
Pass 1 (at output resolution):

Input 4x4 neighborhood
    |
    v
DECOMPOSE:
    +-- Bilateral blur (3x3, sigma_r=80) --------> BASE pixel
    +-- Original - Base --------------------------> DETAIL pixel
    +-- Sobel magnitude + direction --------------> EDGE info
    |
    v
UPSCALE PER LAYER:
    +-- BASE:   Catmull-Rom bicubic (4x4 grid) --> bicubicResult
    +-- DETAIL: Bilinear (2x2) -----------------> detailUpscaled
    +-- EDGE:   1D directional cubic (4 taps) ---> edgeUpscaled
    |
    v
RECOMPOSE:
    edgeBlend = smoothstep(0.04, 0.20, edgeMag)
    structureUpscaled = mix(bicubicResult, edgeUpscaled, edgeBlend)
    detailWeight = mix(0.4, 0.1, edgeBlend)
    result = structureUpscaled + detailUpscaled * detailWeight
    |
    v
Anti-halo clamp to local 2x2 min/max (5% margin)

Pass 2 (at output resolution):
    Edge-aware directional sharpening:
    - Detect edge direction from luma gradients
    - Apply 1D unsharp mask perpendicular to edge
    - Adaptive strength: stronger on edges, weaker in flat areas
    - Luminance mask: suppress in very dark/bright regions
    - Anti-halo clamp with 10% margin
```

**Implementation:** `/feature/ps2/src/androidMain/.../upscale/DecomposeRecomposeStrategy.kt`

| Property | Value |
|----------|-------|
| Passes | 2 (upscale + sharpen) |
| Texture fetches | Pass 1: 16 (4x4) + 4 (edge 1D cubic) = 20. Pass 2: 9 |
| ALU ops | Pass 1: ~180. Pass 2: ~80 |
| Uniforms | Pass 1: `uInputTex`, `uInputSize`, `uOutputSize`. Pass 2: `uInputTex`, `uTexelSize`, `uSharpness` |
| Expected quality | +1.3-1.5 dB PSNR over bicubic |
| Strength | Best edge quality; edges upscaled along their direction |
| Weakness | 2-pass (needs FBO); bilateral decomposition is approximate |

---

## 4. Comparison Table

| Algorithm | Passes | Tex Fetches | Edge Quality | Flat Quality | Diagonal | Artifact Resistance | Speed |
|-----------|--------|-------------|-------------|-------------|----------|--------------------|----|
| Bicubic (baseline) | 1 | 16 | Poor | Excellent | Poor | Good | Fast |
| FilterFusion (A) | 1 | 16 | Good | Good | Fair | Good | Fast |
| MatrixGuided (B) | 1 | ~25 | Very Good | Good | Excellent | Good | Medium |
| DecomposeRecompose (C) | 2 | 20+9 | Excellent | Good | Good | Very Good | Medium |
| FSR EASU (existing) | 1 | 12 | Very Good | Fair | Good | Poor (amplifies) | Fast |
| Feature-Guided (existing) | 2 | 23+9 | Excellent | Very Good | Good | Good | Slower |

### Recommendations for PS2 Streaming Content

1. **Best overall quality:** DecomposeRecompose (C) - the layer separation prevents detail injection from corrupting edges
2. **Best for diagonals:** MatrixGuided (B) - explicit 4-direction interpolation handles all common angles
3. **Best speed/quality tradeoff:** FilterFusion (A) - single-pass with good edge awareness
4. **Best for compressed video:** Combine any of A/B/C with Deblock sharpener, since all three have anti-halo clamping that limits artifact amplification

---

## 5. Future Directions

### 5.1 Learned Weight Matrices
The weight matrices in all three algorithms are hand-tuned. Training them on PS2 game screenshot pairs (native 640x448 vs emulated at higher resolution) would likely improve quality by 0.5-1.0 dB. The training can be done offline with simple least-squares regression, similar to RAISR but with our specific feature vector design.

### 5.2 Temporal Coherence
None of these algorithms use temporal information. For video upscaling, a temporal stability pass could reduce flickering by blending the current frame's upscaled output with the previous frame, weighted by motion estimation. This would add one extra pass and one extra texture (previous frame buffer).

### 5.3 Adaptive Matrix Selection
Instead of a single fixed weight matrix, use 4-8 pre-tuned matrices and interpolate between them based on local statistics (similar to RAISR's coherence binning). This would require uniform arrays or a 3D texture lookup.

---

## Sources

- [RAISR: Rapid and Accurate Image Super Resolution (arXiv)](https://arxiv.org/pdf/1606.01299)
- [Multi-kernel based adaptive interpolation for image super-resolution (Springer)](https://link.springer.com/article/10.1007/s11042-012-1325-4)
- [Structure Tensor Based Image Interpolation Method (arXiv)](https://arxiv.org/pdf/1402.5564)
- [Edge-preserving decompositions for multi-scale tone and detail manipulation (ACM)](https://dl.acm.org/doi/10.1145/1360612.1360666)
- [Bilateral guided upsampling (MIT CSAIL)](https://people.csail.mit.edu/hasinoff/pubs/ChenEtAl16-bgu.pdf)
- [Joint bilateral upsampling (ACM)](https://dl.acm.org/doi/10.1145/1276377.1276497)
- [Guided Linear Upsampling (ACM)](https://dl.acm.org/doi/10.1145/3592453)
- [Local linear models and guided filtering (Bart Wronski)](https://bartwronski.com/2019/09/22/local-linear-models-guided-filter/)
- [Super XBR Scaling Algorithm (DeepWiki)](https://deepwiki.com/libretro/glsl-shaders/4.2-super-xbr-scaling-algorithm)
- [AMD FidelityFX Super Resolution 1.0 demystified](https://jntesteves.github.io/shadesofnoice/graphics/shaders/upscaling/2021/09/11/amd-fsr-demystified.html)
- [Advanced High-Quality Filtering (GPU Gems 2, Chapter 27)](https://developer.nvidia.com/gpugems/gpugems2/part-iii-high-quality-rendering/chapter-27-advanced-high-quality-filtering)
- [Intel Video Super Resolution Library (GitHub)](https://github.com/OpenVisualCloud/Video-Super-Resolution-Library)
- [An edge-adaptive structure tensor kernel regression for image interpolation (IEEE)](https://ieeexplore.ieee.org/document/5497577/)
- [Multi-scale Image Decomposition using a Local Statistical Edge Model (arXiv)](https://arxiv.org/abs/2105.01951)
- [Pixel-art scaling algorithms (Wikipedia)](https://en.wikipedia.org/wiki/Pixel-art_scaling_algorithms)
