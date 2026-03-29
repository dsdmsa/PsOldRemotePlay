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
 * Desktop video renderer (JavaCV implementation).
 * PLACEHOLDER: Currently logs packets. Real H.264 decode requires FFmpeg integration.
 * TODO: Integrate org.bytedeco.javacv for H.264 → ImageBitmap conversion
 */
class JvmVideoRenderer(private val logger: PremoLogger) : VideoRenderer {
    private val _currentFrame = MutableStateFlow<androidx.compose.ui.graphics.ImageBitmap?>(null)
    override val currentFrame: StateFlow<androidx.compose.ui.graphics.ImageBitmap?> = _currentFrame.asStateFlow()

    private var packetCount = 0

    override suspend fun start() = withContext(Dispatchers.IO) {
        logger.log("VIDEO", "[DESKTOP] Video renderer initialized")
    }

    override suspend fun onStreamPacket(header: ByteArray, payload: ByteArray, isEncrypted: Boolean) =
        withContext(Dispatchers.IO) {
            val magic = header[1].toInt() and 0xFF
            packetCount++

            when (magic) {
                0xFF, 0xFE -> {
                    if (packetCount % 30 == 0) {
                        logger.log("VIDEO", "[DESKTOP] H.264 frame ${packetCount}: ${payload.size} bytes")
                    }
                    // TODO: Decode H.264 NAL data and update _currentFrame
                }
                0xFD -> logger.log("VIDEO", "[DESKTOP] Flush signal")
                0xFB -> logger.log("VIDEO", "[DESKTOP] MPEG4 (skipped)")
                else -> logger.log("VIDEO", "[DESKTOP] Unknown magic: ${"%02X".format(magic)}")
            }
        }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        logger.log("VIDEO", "[DESKTOP] Video renderer stopped ($packetCount packets)")
        _currentFrame.value = null
    }
}
