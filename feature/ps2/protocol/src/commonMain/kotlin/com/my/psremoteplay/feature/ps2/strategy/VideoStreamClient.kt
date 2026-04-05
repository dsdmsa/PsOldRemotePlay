package com.my.psremoteplay.feature.ps2.strategy

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow

/**
 * Strategy for the client-side video pipeline: transport → decode → display.
 * Each implementation handles receive + decode internally.
 * Produces decoded frames as ImageBitmap via StateFlow.
 */
interface VideoStreamClient {
    /** Human-readable name for logging/UI */
    val name: String

    /** Decoded video frames ready for display (null for Surface-based rendering) */
    val currentFrame: StateFlow<ImageBitmap?>

    /** Whether this client renders directly to a hardware Surface (bypassing Compose) */
    val usesSurfaceRendering: Boolean get() = false

    /** Number of decoded frames (for Surface-based clients that don't emit ImageBitmap) */
    val decodedFrameCount: Long get() = 0

    /** Connect to the server video stream */
    fun start(serverIp: String, config: StreamConfig): Boolean

    /** Disconnect from the video stream */
    fun stop()

    fun isReceiving(): Boolean
}
