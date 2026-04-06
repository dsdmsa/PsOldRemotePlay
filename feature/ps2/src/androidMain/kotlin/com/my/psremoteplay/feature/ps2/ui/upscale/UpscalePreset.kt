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
    DEBLOCK("Deblock+Bicubic", "H.264 artifact removal + bicubic. Best for compressed video.");
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
