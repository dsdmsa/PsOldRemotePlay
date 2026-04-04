package com.my.psremoteplay.feature.ps2.strategy.client

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import org.jetbrains.skia.Image as SkiaImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * JavaCV RTP/UDP client: receives H.264 via RTP, decodes via FFmpeg.
 * Pairs with JavaCvRtpServer.
 */
class JavaCvRtpClient(private val logger: Logger) : VideoStreamClient {
    override val name = "H.264/RTP"

    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    override val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    @Volatile private var receiving = false
    private var readerThread: Thread? = null

    override fun start(serverIp: String, config: StreamConfig): Boolean {
        return try {
            receiving = true

            readerThread = Thread {
                var grabber: FFmpegFrameGrabber? = null
                try {
                    val converter = Java2DFrameConverter()
                    val rtpUrl = "rtp://$serverIp:${config.videoPort}"

                    grabber = FFmpegFrameGrabber(rtpUrl).apply {
                        format = "rtp"
                        setOption("fflags", "nobuffer")
                        setOption("flags", "low_delay")
                        setOption("analyzeduration", "100000")
                        setOption("probesize", "32768")
                        setOption("reorder_queue_size", "0")
                    }

                    logger.log(name, "Connecting to $rtpUrl...")
                    grabber.start()
                    logger.log(name, "Connected (${grabber.imageWidth}x${grabber.imageHeight})")

                    while (receiving) {
                        val frame = grabber.grabImage() ?: continue
                        val bi = converter.convert(frame) ?: continue
                        val baos = ByteArrayOutputStream(65536)
                        ImageIO.write(bi, "jpeg", baos)
                        val skiaImage = SkiaImage.makeFromEncoded(baos.toByteArray())
                        _currentFrame.value = skiaImage.toComposeImageBitmap()
                    }
                } catch (e: Exception) {
                    if (receiving) logger.error(name, "RTP error: ${e.message}", e)
                } finally {
                    try { grabber?.stop() } catch (_: Exception) {}
                    try { grabber?.release() } catch (_: Exception) {}
                }
            }.apply { isDaemon = true; name = "h264-rtp-client"; start() }

            Thread.sleep(300)
            true
        } catch (e: Exception) {
            logger.error(name, "Start failed: ${e.message}")
            false
        }
    }

    override fun stop() {
        receiving = false
        readerThread?.join(3000)
        readerThread = null
        _currentFrame.value = null
        logger.log(name, "Stopped")
    }

    override fun isReceiving() = receiving
}
