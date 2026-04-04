package com.my.psremoteplay.core.streaming.stubs

import com.my.psremoteplay.core.streaming.codec.*

class StubVideoDecoder : VideoDecoder {
    override suspend fun start(width: Int, height: Int, codecName: String) {}
    override suspend fun decode(nalData: ByteArray): VideoFrame? = null
    override suspend fun flush() {}
    override suspend fun stop() {}
}

class StubAudioDecoder : AudioDecoder {
    override suspend fun start(sampleRate: Int, channels: Int, codecName: String) {}
    override suspend fun decode(data: ByteArray): AudioBuffer? = null
    override suspend fun flush() {}
    override suspend fun stop() {}
}
