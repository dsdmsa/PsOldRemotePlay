package com.my.psremoteplay.core.model.codec

data class AudioBuffer(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int = 16,
    val samples: ByteArray,
    val timestamp: Long = 0
)
