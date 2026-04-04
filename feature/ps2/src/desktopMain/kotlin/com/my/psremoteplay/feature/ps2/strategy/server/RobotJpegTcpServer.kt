package com.my.psremoteplay.feature.ps2.strategy.server

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.protocol.Ps2Protocol
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamServer
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Original approach: Java Robot screen capture → JPEG encode → TCP via Ps2Protocol.
 * Reliable but highest latency (~50-150ms).
 */
class RobotJpegTcpServer(private val logger: Logger) : VideoStreamServer {
    override val name = "MJPEG/TCP"

    @Volatile private var streaming = false
    private var captureThread: Thread? = null
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    private val clients = CopyOnWriteArrayList<OutputStream>()

    override fun start(config: StreamConfig) {
        if (streaming) return
        streaming = true

        // Start TCP server
        serverSocket = ServerSocket(config.videoPort)
        acceptThread = Thread {
            while (streaming) {
                try {
                    val socket = serverSocket?.accept() ?: break
                    logger.log(name, "Client connected: ${socket.remoteSocketAddress}")
                    clients.add(socket.getOutputStream())
                } catch (e: Exception) {
                    if (streaming) logger.error(name, "Accept error: ${e.message}")
                }
            }
        }.apply { isDaemon = true; name = "tcp-accept"; start() }

        // Start capture + encode + broadcast loop
        captureThread = Thread {
            try {
                val robot = Robot()
                val screenSize = Toolkit.getDefaultToolkit().screenSize
                val captureRect = Rectangle(screenSize)
                val jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
                val jpegParams = jpegWriter.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = 0.6f
                }
                val frameIntervalMs = 1000L / config.fps

                logger.log(name, "Capture started (${config.width}x${config.height} JPEG/TCP)")

                while (streaming) {
                    val t0 = System.currentTimeMillis()

                    val screenshot = robot.createScreenCapture(captureRect)
                    val scaled = BufferedImage(config.width, config.height, BufferedImage.TYPE_INT_RGB)
                    scaled.createGraphics().apply {
                        drawImage(screenshot, 0, 0, config.width, config.height, null)
                        dispose()
                    }

                    val baos = ByteArrayOutputStream(65536)
                    val ios = ImageIO.createImageOutputStream(baos)
                    jpegWriter.output = ios
                    jpegWriter.write(null, IIOImage(scaled, null, null), jpegParams)
                    ios.flush(); ios.close()

                    val jpegData = baos.toByteArray()
                    val frame = Ps2Protocol.buildFrame(Ps2Protocol.VIDEO_FRAME, jpegData)

                    val disconnected = mutableListOf<OutputStream>()
                    for (client in clients) {
                        try {
                            client.write(frame)
                            client.flush()
                        } catch (e: Exception) { disconnected.add(client) }
                    }
                    clients.removeAll(disconnected.toSet())

                    val elapsed = System.currentTimeMillis() - t0
                    val sleep = frameIntervalMs - elapsed
                    if (sleep > 0) Thread.sleep(sleep)
                }
                jpegWriter.dispose()
            } catch (e: Exception) {
                if (streaming) logger.error(name, "Capture error: ${e.message}", e)
            }
        }.apply { isDaemon = true; name = "jpeg-tcp-capture"; start() }
    }

    override fun stop() {
        streaming = false
        clients.clear()
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        captureThread?.join(2000)
        captureThread = null
        acceptThread = null
        logger.log(name, "Stopped")
    }

    override fun isStreaming() = streaming
}
