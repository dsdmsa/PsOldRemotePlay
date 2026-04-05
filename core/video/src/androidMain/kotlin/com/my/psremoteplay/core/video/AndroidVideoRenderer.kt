package com.my.psremoteplay.core.video

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.my.psremoteplay.core.model.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AndroidVideoRenderer(private val logger: Logger) : VideoRenderer {
    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    override val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    private var packetCount = 0

    override suspend fun start() {
        logger.log("VIDEO", "[ANDROID] Video renderer initialized")
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
                    val bitmap = BitmapFactory.decodeByteArray(payload, 0, payload.size)
                    if (bitmap != null) {
                        _currentFrame.value = bitmap.asImageBitmap()
                    }
                } catch (e: Exception) {
                    if (packetCount % 30 == 0) {
                        logger.error("VIDEO", "[ANDROID] Failed to decode JPEG frame: ${e.message}")
                    }
                }
            } else {
                if (packetCount % 30 == 0) {
                    logger.log("VIDEO", "[ANDROID] Non-JPEG packet #$packetCount: ${payload.size} bytes")
                }
            }
        }

    override suspend fun stop() {
        logger.log("VIDEO", "[ANDROID] Video renderer stopped ($packetCount packets)")
        _currentFrame.value = null
    }
}
