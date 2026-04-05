package com.my.psremoteplay.core.video.stubs

import androidx.compose.ui.graphics.ImageBitmap
import com.my.psremoteplay.core.model.Logger
import com.my.psremoteplay.core.video.VideoRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoggingVideoRenderer(private val logger: Logger) : VideoRenderer {
    private var packetCount = 0
    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    override val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    override suspend fun start() {
        logger.log("VIDEO", "Video renderer started")
    }

    override suspend fun onStreamPacket(header: ByteArray, payload: ByteArray, isEncrypted: Boolean) {
        packetCount++
        if (packetCount % 30 == 0) {
            logger.log("VIDEO", "Received $packetCount packets, last payload: ${payload.size} bytes")
        }
    }

    override suspend fun stop() {
        logger.log("VIDEO", "Video renderer stopped ($packetCount total packets)")
        packetCount = 0
        _currentFrame.value = null
    }
}
