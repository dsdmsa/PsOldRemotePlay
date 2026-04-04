package com.my.psremoteplay.feature.ps2.strategy

/**
 * Shared configuration for video streaming strategies.
 */
data class StreamConfig(
    val videoPort: Int = 9296,
    val width: Int = 640,
    val height: Int = 448,
    val fps: Int = 30,
    val bitrate: Int = 8_000_000,
    val quality: Int = 5  // JPEG quality (2=best, 31=worst) or codec-specific
)
