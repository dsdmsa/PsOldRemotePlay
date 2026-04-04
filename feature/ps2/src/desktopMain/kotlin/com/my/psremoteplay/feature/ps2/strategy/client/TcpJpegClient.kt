package com.my.psremoteplay.feature.ps2.strategy.client

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.skia.Image as SkiaImage
import java.io.DataInputStream
import java.net.Socket

/**
 * TCP client that receives JPEG frames via Ps2Protocol framing.
 * Pairs with RobotJpegTcpServer.
 */
class TcpJpegClient(private val logger: Logger) : VideoStreamClient {
    override val name = "MJPEG/TCP"

    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    override val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    @Volatile private var receiving = false
    private var socket: Socket? = null
    private var readerThread: Thread? = null

    override fun start(serverIp: String, config: StreamConfig): Boolean {
        return try {
            val s = Socket(serverIp, config.videoPort)
            socket = s
            receiving = true

            readerThread = Thread {
                try {
                    val input = DataInputStream(s.getInputStream())
                    while (receiving && !s.isClosed) {
                        val length = input.readInt()
                        if (length <= 0) continue
                        val type = input.readByte()
                        val payloadSize = length - 1
                        val payload = ByteArray(payloadSize)
                        if (payloadSize > 0) input.readFully(payload)

                        if (type == 0x01.toByte() && payload.size >= 2 &&
                            payload[0] == 0xFF.toByte() && payload[1] == 0xD8.toByte()) {
                            try {
                                _currentFrame.value = SkiaImage.makeFromEncoded(payload).toComposeImageBitmap()
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    if (receiving) logger.error(name, "Read error: ${e.message}")
                }
            }.apply { isDaemon = true; name = "tcp-jpeg-reader"; start() }

            logger.log(name, "Connected to $serverIp:${config.videoPort}")
            true
        } catch (e: Exception) {
            logger.error(name, "Connect failed: ${e.message}")
            false
        }
    }

    override fun stop() {
        receiving = false
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        readerThread?.join(2000)
        readerThread = null
        _currentFrame.value = null
        logger.log(name, "Stopped")
    }

    override fun isReceiving() = receiving && socket?.isClosed == false
}
