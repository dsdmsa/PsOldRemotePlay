package com.my.psremoteplay.feature.ps2.strategy

/**
 * Predefined server+client strategy pairings for easy selection.
 * Each preset names a compatible server/client pair.
 */
enum class StreamingPreset(
    val displayName: String,
    val description: String
) {
    JPEG_TCP(
        "MJPEG over TCP",
        "Original approach. Reliable, higher latency. Robot capture → JPEG → TCP"
    ),
    JPEG_UDP(
        "MJPEG over UDP",
        "Low latency MJPEG. Robot capture → JPEG → raw UDP datagrams"
    ),
    H264_RTP(
        "H.264 over RTP/UDP",
        "Hardware-friendly H.264 via JavaCV. Robot capture → H.264 → RTP/UDP"
    ),
    H264_MPEGTS(
        "H.264 over MPEG-TS/UDP",
        "FFmpeg subprocess. Self-describing stream, no SDP needed"
    ),
    PCSX2_PIPE(
        "PCSX2 Direct Pipe",
        "Zero-capture-latency. Reads from PCSX2's built-in video capture pipe"
    );
}
