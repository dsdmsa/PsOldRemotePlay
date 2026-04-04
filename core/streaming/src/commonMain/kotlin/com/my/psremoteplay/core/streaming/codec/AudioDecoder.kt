package com.my.psremoteplay.core.streaming.codec

interface AudioDecoder {
    suspend fun start(sampleRate: Int, channels: Int, codecName: String = "AAC")
    suspend fun decode(data: ByteArray): AudioBuffer?
    suspend fun flush()
    suspend fun stop()
}
