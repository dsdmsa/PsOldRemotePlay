package com.my.psremoteplay.feature.ps2.strategy.server

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamServer
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Low-latency MJPEG: Robot capture → JPEG encode → raw UDP.
 * Captures the PCSX2 window specifically (by title), not the full desktop.
 */
class RobotJpegUdpServer(private val logger: Logger) : VideoStreamServer {
    override val name = "MJPEG/UDP"

    @Volatile private var streaming = false
    private var captureThread: Thread? = null
    private var socket: DatagramSocket? = null

    private lateinit var targetAddress: InetAddress

    override fun start(config: StreamConfig) {
        if (streaming) return
        streaming = true

        targetAddress = InetAddress.getByName(config.targetIp)
        socket = DatagramSocket().apply { broadcast = true }
        logger.log(name, "Sending video to ${config.targetIp}:${config.videoPort}")

        captureThread = Thread {
            try {
                val robot = Robot()
                val jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
                val jpegParams = jpegWriter.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = 0.6f
                }
                val frameIntervalMs = 1000L / config.fps
                val udpSocket = socket!!

                // Try to find the PCSX2 window bounds
                var captureRect = findPcsx2WindowBounds()
                if (captureRect != null) {
                    logger.log(name, "Capturing PCSX2 window: ${captureRect.width}x${captureRect.height} at (${captureRect.x},${captureRect.y})")
                } else {
                    captureRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
                    logger.log(name, "PCSX2 window not found, capturing full screen")
                }

                logger.log(name, "Streaming JPEG/UDP on port ${config.videoPort}")

                var windowCheckCounter = 0
                while (streaming) {
                    val t0 = System.currentTimeMillis()

                    // Re-check window position every 2 seconds (window may move/resize)
                    windowCheckCounter++
                    if (windowCheckCounter % (config.fps * 2) == 0) {
                        findPcsx2WindowBounds()?.let { captureRect = it }
                    }

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
                    val packet = DatagramPacket(jpegData, jpegData.size, targetAddress, config.videoPort)
                    try { udpSocket.send(packet) } catch (_: Exception) {}

                    val elapsed = System.currentTimeMillis() - t0
                    val sleep = frameIntervalMs - elapsed
                    if (sleep > 0) Thread.sleep(sleep)
                }
                jpegWriter.dispose()
            } catch (e: Exception) {
                if (streaming) logger.error(name, "Error: ${e.message}", e)
            }
        }.apply { isDaemon = true; name = "jpeg-udp-capture"; start() }
    }

    override fun stop() {
        streaming = false
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        captureThread?.join(2000)
        captureThread = null
        logger.log(name, "Stopped")
    }

    override fun isStreaming() = streaming

    /**
     * Find the PCSX2 window bounds using macOS `osascript`.
     * Returns null if PCSX2 window not found.
     */
    private fun findPcsx2WindowBounds(): Rectangle? {
        return try {
            // Use AppleScript to get the frontmost PCSX2 window position and size
            val script = """
                tell application "System Events"
                    set pcsx2 to first process whose name contains "PCSX2"
                    set w to first window of pcsx2
                    set {x, y} to position of w
                    set {width, height} to size of w
                    return "" & x & "," & y & "," & width & "," & height
                end tell
            """.trimIndent()

            val process = ProcessBuilder("osascript", "-e", script)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            if (process.exitValue() != 0) return null

            val parts = output.split(",").map { it.trim().toInt() }
            if (parts.size == 4) {
                Rectangle(parts[0], parts[1], parts[2], parts[3])
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
