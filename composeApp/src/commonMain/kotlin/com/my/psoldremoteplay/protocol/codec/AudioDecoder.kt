package com.my.psoldremoteplay.protocol.codec

/**
 * Audio codec decoder. Converts AAC/ATRAC3 to PCM.
 */
interface AudioDecoder {
    /**
     * Initialize the decoder.
     */
    suspend fun start(sampleRate: Int, channels: Int, codecName: String = "AAC")

    /**
     * Decode an audio packet. Input is typically AAC ADTS or raw AAC.
     * Returns null if samples are not yet available.
     */
    suspend fun decode(data: ByteArray): AudioBuffer?

    /**
     * Flush any pending samples.
     */
    suspend fun flush()

    /**
     * Release decoder resources.
     */
    suspend fun stop()
}
