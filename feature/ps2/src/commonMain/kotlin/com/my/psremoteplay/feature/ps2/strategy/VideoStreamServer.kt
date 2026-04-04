package com.my.psremoteplay.feature.ps2.strategy

/**
 * Strategy for the server-side video pipeline: capture → encode → transport.
 * Each implementation handles the full pipeline internally, since some approaches
 * (JavaCV RTP, FFmpeg subprocess) combine all steps.
 */
interface VideoStreamServer {
    /** Human-readable name for logging/UI */
    val name: String

    /** Start capturing, encoding, and streaming video */
    fun start(config: StreamConfig)

    /** Stop the pipeline */
    fun stop()

    fun isStreaming(): Boolean
}
