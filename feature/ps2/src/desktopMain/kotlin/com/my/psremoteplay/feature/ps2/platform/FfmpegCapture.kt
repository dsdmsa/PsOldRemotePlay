package com.my.psremoteplay.feature.ps2.platform

import com.my.psremoteplay.core.streaming.Logger
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

class FfmpegCapture(private val logger: Logger) {
    private var captureThread: Thread? = null
    @Volatile private var capturing = false

    /**
     * Start screen capture using Java Robot API.
     * Captures the screen at ~30fps and encodes each frame as JPEG.
     * @param screenIndex unused (captures primary screen)
     */
    fun start(screenIndex: String = "1", onJpegFrame: (ByteArray) -> Unit) {
        if (capturing) {
            logger.log("CAPTURE", "Already capturing")
            return
        }
        capturing = true

        captureThread = Thread {
            try {
                val robot = Robot()
                val screenSize = Toolkit.getDefaultToolkit().screenSize
                val captureRect = Rectangle(screenSize)
                val targetWidth = 640
                val targetHeight = 448
                val frameIntervalMs = 33L // ~30fps

                // Set up JPEG writer with quality control
                val jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
                val jpegParams = jpegWriter.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = 0.6f // 0.0=worst, 1.0=best
                }

                logger.log("CAPTURE", "Screen capture started (${screenSize.width}x${screenSize.height} -> ${targetWidth}x${targetHeight})")

                while (capturing) {
                    val frameStart = System.currentTimeMillis()

                    val screenshot = robot.createScreenCapture(captureRect)
                    val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
                    val g = scaled.createGraphics()
                    g.drawImage(screenshot, 0, 0, targetWidth, targetHeight, null)
                    g.dispose()

                    val baos = ByteArrayOutputStream(65536)
                    val ios = ImageIO.createImageOutputStream(baos)
                    jpegWriter.output = ios
                    jpegWriter.write(null, IIOImage(scaled, null, null), jpegParams)
                    ios.flush()
                    ios.close()

                    onJpegFrame(baos.toByteArray())

                    val elapsed = System.currentTimeMillis() - frameStart
                    val sleepTime = frameIntervalMs - elapsed
                    if (sleepTime > 0) Thread.sleep(sleepTime)
                }

                jpegWriter.dispose()
            } catch (e: Exception) {
                if (capturing) {
                    logger.error("CAPTURE", "Capture error: ${e.message}", e)
                }
            }
        }.apply { isDaemon = true; name = "screen-capture"; start() }

        logger.log("CAPTURE", "Capture started")
    }

    fun stop() {
        capturing = false
        captureThread?.join(2000)
        captureThread = null
        logger.log("CAPTURE", "Capture stopped")
    }

    fun isCapturing(): Boolean = capturing && captureThread?.isAlive == true
}
