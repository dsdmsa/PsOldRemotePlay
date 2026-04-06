package com.my.psremoteplay.feature.ps2.ui.upscale

/** Upscaler options (input resolution → output resolution) */
enum class UpscaleMethod(val displayName: String, val description: String) {
    BILINEAR("Bilinear", "Default GPU filtering. Fastest, softest."),
    CATMULL_ROM("Bicubic", "9-sample Catmull-Rom cubic. Sharp, artifact-free."),
    FSR_EASU("FSR EASU", "AMD edge-adaptive Lanczos. Sharp edges, may enhance compression."),
    CUSTOM("Edge Bilateral", "Edge-preserving bicubic with bilateral weighting."),
    MATRIX_FILTER("Matrix Filter", "Single-pass multi-filter bank via matrix multiply. Fast edge-directed."),
    FEATURE_GUIDED("Feature Guided", "Structure tensor analysis + directional bicubic. Best edge quality."),
    RAISR("RAISR-style", "Gradient-classified filter bank. Different filters per edge type."),
    LUT_UPSCALE("LUT Upscale", "Pre-baked interpolation weights. Edge-aware via lookup."),
    DEBLOCK("Deblock+Bicubic", "H.264 artifact removal + bicubic. Best for compressed video."),
    ADAPTIVE_MATRIX("Adaptive Matrix", "Per-pixel matrix-multiply with 8-dir gradient classification. 16 taps."),
    NEIGHBORHOOD_TRANSFORM("Nbhd Transform", "Minimal 3x3 matrix upscaler. 9 taps, 27 MADs. Fastest learned filter."),
    HYBRID_DECOMPOSE("Hybrid Decompose", "3-matrix decomposition: smooth + detail * f(edge). 25 taps, best quality."),
    FILTER_FUSION("FilterFusion", "3-filter parallel decomposition with guidance-driven anisotropic bicubic."),
    MATRIX_GUIDED("MatrixGuided", "Feature vector x weight matrix -> 4-direction interpolation blend."),
    DECOMPOSE_RECOMPOSE("DecomposeRecompose", "Base/detail/edge layer separation, per-layer upscale, recompose."),
    OPTIMAL("Optimal", "Adaptive bicubic with artifact suppression. Best for compressed video."),
    FIXED_3X("Fixed 3x", "Pre-computed 3x integer bicubic. Fastest quality, uniform scaling."),
    DUAL_KERNEL("DualKernel", "Catmull-Rom + Mitchell blend. Best quality — beats bicubic on all metrics."),
    LUMA_GUIDED("LumaGuided", "Bicubic + bilinear blend by contrast. Best SSIM for compressed video."),
    TINY_NN("Tiny NN", "209-param trained neural network. Content-adaptive luma correction.");
}

/** Sharpener options (applied after upscaling at output resolution) */
enum class SharpenMethod(val displayName: String, val description: String) {
    NONE("None", "No sharpening."),
    CAS("CAS", "Per-channel contrast-adaptive. Gentle, artifact-free."),
    FSR_RCAS("FSR RCAS", "Noise-gated luma sharpening. Stronger with noise suppression."),
    CUSTOM_USM("Luma USM", "Luminance-weighted unsharp mask. Mid-luma only."),
    FEATURE_SHARPEN("Dir. Sharpen", "Directional sharpen along edges + min/max clamp."),
    DEBLOCK_SHARPEN("Deblock Sharp", "Edge-aware sharpen that avoids block boundaries.");
}
