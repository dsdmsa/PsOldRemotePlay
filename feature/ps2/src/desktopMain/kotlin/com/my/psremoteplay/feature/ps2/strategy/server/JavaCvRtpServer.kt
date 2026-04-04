package com.my.psremoteplay.feature.ps2.strategy.server

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamServer
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage

/**
 * Robot capture → H.264 encode via JavaCV/FFmpeg → RTP/UDP output.
 * Requires client to use matching RTP receiver (JavaCvRtpClient).
 * Note: RTP requires SDP negotiation — client must know stream params.
 */
class JavaCvRtpServer(private val logger: Logger) : VideoStreamServer {
    override val name = "H.264/RTP"

    @Volatile private var streaming = false
    private var streamThread: Thread? = null

    override fun start(config: StreamConfig) {
        if (streaming) return
        streaming = true

        streamThread = Thread {
            var recorder: FFmpegFrameRecorder? = null
            try {
                val robot = Robot()
                val screenSize = Toolkit.getDefaultToolkit().screenSize
                val captureRect = Rectangle(screenSize)
                val converter = Java2DFrameConverter()
                val rtpUrl = "rtp://0.0.0.0:${config.videoPort}"

                recorder = FFmpegFrameRecorder(rtpUrl, config.width, config.height).apply {
                    format = "rtp"
                    videoCodec = avcodec.AV_CODEC_ID_H264
                    frameRate = config.fps.toDouble()
                    videoBitrate = config.bitrate
                    setVideoOption("preset", "ultrafast")
                    setVideoOption("tune", "zerolatency")
                    setVideoOption("profile", "baseline")
                    gopSize = config.fps * 2
                    pixelFormat = avutil.AV_PIX_FMT_YUV420P
                    setVideoOption("bf", "0")
                    setVideoOption("refs", "1")
                    setVideoOption("rc-lookahead", "0")
                    setOption("pkt_size", "1200")
                }
                recorder.start()
                logger.log(name, "RTP stream on port ${config.videoPort} (${config.width}x${config.height})")

                val frameIntervalMs = 1000L / config.fps
                while (streaming) {
                    val t0 = System.currentTimeMillis()
                    val screenshot = robot.createScreenCapture(captureRect)
                    val scaled = BufferedImage(config.width, config.height, BufferedImage.TYPE_3BYTE_BGR)
                    scaled.createGraphics().apply {
                        drawImage(screenshot, 0, 0, config.width, config.height, null)
                        dispose()
                    }
                    recorder.record(converter.convert(scaled))

                    val elapsed = System.currentTimeMillis() - t0
                    val sleep = frameIntervalMs - elapsed
                    if (sleep > 0) Thread.sleep(sleep)
                }
            } catch (e: Exception) {
                if (streaming) logger.error(name, "Error: ${e.message}", e)
            } finally {
                try { recorder?.stop() } catch (_: Exception) {}
                try { recorder?.release() } catch (_: Exception) {}
            }
        }.apply { isDaemon = true; name = "h264-rtp-server"; start() }
    }

    override fun stop() {
        streaming = false
        streamThread?.join(3000)
        streamThread = null
        logger.log(name, "Stopped")
    }

    override fun isStreaming() = streaming
}
