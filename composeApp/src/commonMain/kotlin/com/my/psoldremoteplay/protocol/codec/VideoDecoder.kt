package com.my.psoldremoteplay.protocol.codec

/**
 * Video codec decoder. Converts H.264/MPEG4 NAL units to decoded frames.
 */
interface VideoDecoder {
    /**
     * Initialize the decoder. Called before any decode() calls.
     * May be called multiple times if format changes.
     */
    suspend fun start(width: Int, height: Int, codecName: String = "H.264")

    /**
     * Decode a video packet. Input is typically Annex B H.264 NAL units.
     * Returns null if frame is not yet ready (e.g., for P-frames without prior I-frame).
     */
    suspend fun decode(nalData: ByteArray): VideoFrame?

    /**
     * Flush any pending frames (e.g., after 0xFD "restore" signal).
     */
    suspend fun flush()

    /**
     * Release decoder resources.
     */
    suspend fun stop()
}
