package com.my.psremoteplay.feature.ps2.ui.upscale

/**
 * Available upscaling presets for the PS2 streaming client.
 */
enum class UpscalePreset(
    val displayName: String,
    val description: String
) {
    NONE(
        displayName = "None (Bilinear)",
        description = "Default bilinear filtering. Lowest latency, no sharpening."
    ),
    FSR(
        displayName = "FSR 1.0",
        description = "AMD FidelityFX Super Resolution. Edge-adaptive spatial upscale + RCAS sharpening."
    ),
    SGSR(
        displayName = "Snapdragon GSR",
        description = "Qualcomm Game Super Resolution. Optimized for Adreno GPUs."
    ),
    CATMULL_ROM_CAS(
        displayName = "Bicubic + CAS",
        description = "Bicubic interpolation with contrast-adaptive sharpening."
    )
}
