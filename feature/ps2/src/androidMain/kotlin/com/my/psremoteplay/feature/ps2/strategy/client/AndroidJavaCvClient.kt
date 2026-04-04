package com.my.psremoteplay.feature.ps2.strategy.client

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.FFmpegFrameGrabber

/**
 * Android JavaCV client: receives H.264 via RTP or MPEG-TS/UDP, decodes via FFmpeg.
 * Set format to "rtp" for JavaCvRtpServer or "mpegts" for FfmpegMpegTsServer.
 */
class AndroidJavaCvClient(
    private val logger: Logger,
    private val streamFormat: String = "mpegts" // "rtp" or "mpegts"
) : VideoStreamClient {
    override val name = if (streamFormat == "rtp") "H.264/RTP" else "H.264/MPEG-TS"

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
                    val converter = AndroidFrameConverter()
                    val protocol = if (streamFormat == "rtp") "rtp" else "udp"
                    val url = "$protocol://$serverIp:${config.videoPort}"

                    grabber = FFmpegFrameGrabber(url).apply {
                        format = streamFormat
                        setOption("fflags", "nobuffer")
                        setOption("flags", "low_delay")
                        setOption("analyzeduration", if (streamFormat == "rtp") "100000" else "500000")
                        setOption("probesize", if (streamFormat == "rtp") "32768" else "65536")
                        if (streamFormat == "rtp") setOption("reorder_queue_size", "0")
                    }

                    logger.log(name, "Connecting to $url...")
                    grabber.start()
                    logger.log(name, "Connected (${grabber.imageWidth}x${grabber.imageHeight})")

                    while (receiving) {
                        val frame = grabber.grabImage() ?: continue
                        val bitmap = converter.convert(frame) ?: continue
                        _currentFrame.value = bitmap.asImageBitmap()
                    }
                } catch (e: Exception) {
                    if (receiving) logger.error(name, "Stream error: ${e.message}", e)
                } finally {
                    try { grabber?.stop() } catch (_: Exception) {}
                    try { grabber?.release() } catch (_: Exception) {}
                }
            }.apply { isDaemon = true; name = "android-javacv-client"; start() }

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
    }

    override fun isReceiving() = receiving
}
