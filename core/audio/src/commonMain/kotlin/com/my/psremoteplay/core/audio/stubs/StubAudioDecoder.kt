package com.my.psremoteplay.core.audio.stubs

import com.my.psremoteplay.core.audio.AudioDecoder
import com.my.psremoteplay.core.model.codec.AudioBuffer

class StubAudioDecoder : AudioDecoder {
    override suspend fun start(sampleRate: Int, channels: Int, codecName: String) {}
    override suspend fun decode(data: ByteArray): AudioBuffer? = null
    override suspend fun flush() {}
    override suspend fun stop() {}
}
