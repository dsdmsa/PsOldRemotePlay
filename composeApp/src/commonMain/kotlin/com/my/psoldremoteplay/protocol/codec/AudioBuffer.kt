package com.my.psoldremoteplay.protocol.codec

/**
 * Decoded audio PCM data.
 */
data class AudioBuffer(
    val sampleRate: Int,             // Hz (44100, 48000, etc.)
    val channels: Int,               // 1 = mono, 2 = stereo
    val bitsPerSample: Int = 16,     // 16, 24, 32 bits
    val samples: ByteArray,          // PCM data (interleaved if multi-channel)
    val timestamp: Long = 0          // PTS in milliseconds
)
