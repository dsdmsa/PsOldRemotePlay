package com.my.psremoteplay.core.video

import com.my.psremoteplay.core.model.codec.VideoFrame

interface VideoDecoder {
    suspend fun start(width: Int, height: Int, codecName: String = "H.264")
    suspend fun decode(nalData: ByteArray): VideoFrame?
    suspend fun flush()
    suspend fun stop()
}
