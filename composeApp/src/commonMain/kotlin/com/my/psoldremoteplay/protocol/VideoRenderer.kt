package com.my.psoldremoteplay.protocol

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow

/**
 * Video rendering handler. Decodes H.264 packets and renders frames.
 */
interface VideoRenderer {
    /**
     * Initialize the video renderer. Called once at stream start.
     */
    suspend fun start()

    /**
     * Process a raw video stream packet. Payload is already decrypted.
     */
    suspend fun onStreamPacket(header: ByteArray, payload: ByteArray, isEncrypted: Boolean)

    /**
     * Shutdown and release resources.
     */
    suspend fun stop()

    /**
     * Current decoded frame, if available. UI observes this to render.
     * Null if no frame decoded yet.
     */
    val currentFrame: StateFlow<ImageBitmap?>
}
