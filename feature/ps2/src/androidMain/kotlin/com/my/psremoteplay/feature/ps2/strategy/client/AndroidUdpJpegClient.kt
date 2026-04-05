package com.my.psremoteplay.feature.ps2.strategy.client

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket

/**
 * Android UDP client: receives JPEG datagrams, decodes via BitmapFactory.
 * Pairs with RobotJpegUdpServer or Pcsx2PipeServer.
 */
class AndroidUdpJpegClient(private val logger: Logger) : VideoStreamClient {
    override val name = "MJPEG/UDP"

    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    override val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    @Volatile private var receiving = false
    private var socket: DatagramSocket? = null
    private var readerThread: Thread? = null

    override fun start(serverIp: String, config: StreamConfig): Boolean {
        return try {
            val s = DatagramSocket(config.videoPort)
            s.soTimeout = 5000
            socket = s
            receiving = true

            readerThread = Thread {
                val buf = ByteArray(65536)
                var frameCount = 0L
                var totalDecodeMs = 0L
                val startTime = System.currentTimeMillis()
                try {
                    while (receiving) {
                        val packet = DatagramPacket(buf, buf.size)
                        try {
                            s.receive(packet)
                        } catch (_: java.net.SocketTimeoutException) { continue }

                        val data = packet.data.copyOf(packet.length)
                        if (data.size >= 2 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte()) {
                            val t0 = System.currentTimeMillis()
                            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                            val decodeMs = System.currentTimeMillis() - t0
                            if (bitmap != null) {
                                _currentFrame.value = bitmap.asImageBitmap()
                                frameCount++
                                totalDecodeMs += decodeMs
                                if (frameCount % 150 == 0L) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    val avgDecode = totalDecodeMs / frameCount
                                    val fps = frameCount * 1000 / elapsed
                                    logger.log("PERF", "decode=${avgDecode}ms fps=$fps size=${data.size / 1024}KB frames=$frameCount")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (receiving) logger.error(name, "Receive error: ${e.message}")
                }
            }.apply { isDaemon = true; name = "udp-jpeg-reader"; start() }

            logger.log(name, "Listening on UDP port ${config.videoPort}")
            true
        } catch (e: Exception) {
            logger.error(name, "Bind failed: ${e.message}")
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
    }

    override fun isReceiving() = receiving
}
