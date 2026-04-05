package com.my.psremoteplay.core.audio

import com.my.psremoteplay.core.model.codec.AudioBuffer

interface AudioDecoder {
    suspend fun start(sampleRate: Int, channels: Int, codecName: String = "AAC")
    suspend fun decode(data: ByteArray): AudioBuffer?
    suspend fun flush()
    suspend fun stop()
}
