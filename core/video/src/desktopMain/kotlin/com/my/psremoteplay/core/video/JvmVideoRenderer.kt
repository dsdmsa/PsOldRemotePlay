package com.my.psremoteplay.core.video

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.my.psremoteplay.core.model.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

class JvmVideoRenderer(private val logger: Logger) : VideoRenderer {
    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    override val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    private var packetCount = 0

    override suspend fun start() = withContext(Dispatchers.IO) {
        logger.log("VIDEO", "[DESKTOP] Video renderer initialized")
    }

    override suspend fun onStreamPacket(header: ByteArray, payload: ByteArray, isEncrypted: Boolean) =
        withContext(Dispatchers.IO) {
            packetCount++

            // Detect JPEG data (SOI marker: 0xFF 0xD8)
            if (payload.size >= 2 &&
                payload[0] == 0xFF.toByte() &&
                payload[1] == 0xD8.toByte()
            ) {
                try {
                    val skiaImage = SkiaImage.makeFromEncoded(payload)
                    _currentFrame.value = skiaImage.toComposeImageBitmap()
                } catch (e: Exception) {
                    if (packetCount % 30 == 0) {
                        logger.error("VIDEO", "[DESKTOP] Failed to decode JPEG frame: ${e.message}")
                    }
                }
            } else {
                // Legacy H.264 path - log only
                if (packetCount % 30 == 0) {
                    logger.log("VIDEO", "[DESKTOP] Non-JPEG packet #$packetCount: ${payload.size} bytes")
                }
            }
        }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        logger.log("VIDEO", "[DESKTOP] Video renderer stopped ($packetCount packets)")
        _currentFrame.value = null
    }
}
