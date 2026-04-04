package com.my.psremoteplay.core.streaming.codec

interface VideoDecoder {
    suspend fun start(width: Int, height: Int, codecName: String = "H.264")
    suspend fun decode(nalData: ByteArray): VideoFrame?
    suspend fun flush()
    suspend fun stop()
}
