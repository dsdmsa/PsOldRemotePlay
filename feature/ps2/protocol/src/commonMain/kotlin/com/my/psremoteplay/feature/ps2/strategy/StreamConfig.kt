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
    val quality: Int = 5,  // JPEG quality (2=best, 31=worst) or codec-specific
    val targetIp: String = "127.0.0.1",  // Where to send UDP video (client IP)
    // H.264 encoding parameters (used by H264_HW preset)
    val keyframeInterval: Int = 30,  // frames between IDR keyframes (1s at 30fps)
    val profile: String = "baseline",  // H.264 profile (baseline = max device compat)
    val hwAccel: Boolean = true  // prefer hardware encoding when available
)
