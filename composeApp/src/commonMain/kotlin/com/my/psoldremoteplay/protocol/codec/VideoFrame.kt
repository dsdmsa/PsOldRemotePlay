package com.my.psoldremoteplay.protocol.codec

/**
 * Decoded video frame data. Format and plane layout may vary by codec and platform.
 *
 * For H.264 YUV420P:
 * - planes[0] = Y plane (luminance)
 * - planes[1] = U plane (chrominance, 1/4 resolution)
 * - planes[2] = V plane (chrominance, 1/4 resolution)
 */
data class VideoFrame(
    val width: Int,
    val height: Int,
    val format: String = "YUV420P",  // YUV420P, NV12, RGBA, etc.
    val planes: List<ByteArray>,     // Plane data (format-dependent)
    val strides: List<Int> = emptyList(),  // Bytes per row per plane (optional)
    val timestamp: Long = 0           // PTS in milliseconds
)
