package com.my.psremoteplay.core.video.stubs

import com.my.psremoteplay.core.model.codec.VideoFrame
import com.my.psremoteplay.core.video.VideoDecoder

class StubVideoDecoder : VideoDecoder {
    override suspend fun start(width: Int, height: Int, codecName: String) {}
    override suspend fun decode(nalData: ByteArray): VideoFrame? = null
    override suspend fun flush() {}
    override suspend fun stop() {}
}
