package com.my.psoldremoteplay.protocol

import com.my.psoldremoteplay.protocol.codec.AudioBuffer
import com.my.psoldremoteplay.protocol.codec.AudioDecoder
import com.my.psoldremoteplay.protocol.codec.VideoDecoder
import com.my.psoldremoteplay.protocol.codec.VideoFrame

/**
 * Stub video decoder — returns null for all frames.
 * Real implementations (JavaCV, MediaCodec) will override this.
 */
class StubVideoDecoder : VideoDecoder {
    override suspend fun start(width: Int, height: Int, codecName: String) {}
    override suspend fun decode(nalData: ByteArray): VideoFrame? = null
    override suspend fun flush() {}
    override suspend fun stop() {}
}

/**
 * Stub audio decoder — returns null for all buffers.
 */
class StubAudioDecoder : AudioDecoder {
    override suspend fun start(sampleRate: Int, channels: Int, codecName: String) {}
    override suspend fun decode(data: ByteArray): AudioBuffer? = null
    override suspend fun flush() {}
    override suspend fun stop() {}
}
