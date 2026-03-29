package com.my.psoldremoteplay

import androidx.compose.ui.graphics.ImageBitmap
import com.my.psoldremoteplay.protocol.PremoLogger
import com.my.psoldremoteplay.protocol.VideoRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android video renderer (MediaCodec implementation).
 * PLACEHOLDER: Currently logs packets. Real H.264 decode requires MediaCodec integration.
 * TODO: Use android.media.MediaCodec for H.264 → Surface decoding
 */
class AndroidVideoRenderer(private val logger: PremoLogger) : VideoRenderer {
    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    override val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    private var packetCount = 0

    override suspend fun start() = withContext(Dispatchers.IO) {
        logger.log("VIDEO", "[ANDROID] Video renderer initialized")
    }

    override suspend fun onStreamPacket(header: ByteArray, payload: ByteArray, isEncrypted: Boolean) =
        withContext(Dispatchers.IO) {
            val magic = header[1].toInt() and 0xFF
            packetCount++

            when (magic) {
                0xFF, 0xFE -> {
                    if (packetCount % 30 == 0) {
                        logger.log("VIDEO", "[ANDROID] H.264 frame ${packetCount}: ${payload.size} bytes")
                    }
                    // TODO: Queue to MediaCodec decoder
                }
                0xFD -> logger.log("VIDEO", "[ANDROID] Flush signal")
                0xFB -> logger.log("VIDEO", "[ANDROID] MPEG4 (skipped)")
                else -> logger.log("VIDEO", "[ANDROID] Unknown magic: ${"%02X".format(magic)}")
            }
        }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        logger.log("VIDEO", "[ANDROID] Video renderer stopped ($packetCount packets)")
        _currentFrame.value = null
    }
}
