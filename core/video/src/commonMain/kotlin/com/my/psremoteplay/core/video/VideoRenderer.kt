package com.my.psremoteplay.core.video

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow

interface VideoRenderer {
    suspend fun start()
    suspend fun onStreamPacket(header: ByteArray, payload: ByteArray, isEncrypted: Boolean)
    suspend fun stop()
    val currentFrame: StateFlow<ImageBitmap?>
}
