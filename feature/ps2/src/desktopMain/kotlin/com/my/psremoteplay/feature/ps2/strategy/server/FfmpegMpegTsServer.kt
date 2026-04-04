package com.my.psremoteplay.feature.ps2.strategy.server

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamServer

/**
 * FFmpeg as subprocess: capture + H.264 encode + MPEG-TS/UDP output.
 * Self-describing stream (no SDP needed). Lowest integration complexity.
 * FFmpeg handles the entire pipeline natively in C — most efficient.
 */
class FfmpegMpegTsServer(private val logger: Logger) : VideoStreamServer {
    override val name = "H.264/MPEG-TS"

    @Volatile private var streaming = false
    private var process: Process? = null
    private var logThread: Thread? = null

    override fun start(config: StreamConfig) {
        if (streaming) return
        streaming = true

        val udpUrl = "udp://0.0.0.0:${config.videoPort}?pkt_size=1316"

        // Detect platform for capture input
        val os = System.getProperty("os.name").lowercase()
        val captureArgs = when {
            os.contains("mac") -> listOf(
                "-f", "avfoundation",
                "-framerate", config.fps.toString(),
                "-capture_cursor", "0",
                "-i", "1:none"
            )
            os.contains("linux") -> listOf(
                "-f", "x11grab",
                "-framerate", config.fps.toString(),
                "-video_size", "${Toolkit.screenSize.width}x${Toolkit.screenSize.height}",
                "-i", ":0.0"
            )
            else -> {
                logger.error(name, "Unsupported OS: $os")
                streaming = false
                return
            }
        }

        val cmd = listOf("ffmpeg") + captureArgs + listOf(
            "-fps_mode", "cfr",
            "-vf", "scale=${config.width}:${config.height}",
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-tune", "zerolatency",
            "-b:v", config.bitrate.toString(),
            "-g", (config.fps * 2).toString(),
            "-bf", "0",
            "-f", "mpegts",
            udpUrl
        )

        try {
            logger.log(name, "Starting FFmpeg: ${cmd.joinToString(" ")}")
            val pb = ProcessBuilder(cmd)
            pb.redirectError(ProcessBuilder.Redirect.PIPE)
            process = pb.start()

            logThread = Thread {
                process?.errorStream?.bufferedReader()?.forEachLine {
                    logger.log("FFMPEG", it)
                }
            }.apply { isDaemon = true; start() }

            logger.log(name, "MPEG-TS/UDP stream on port ${config.videoPort}")
        } catch (e: Exception) {
            logger.error(name, "Failed to start FFmpeg: ${e.message}", e)
            streaming = false
        }
    }

    override fun stop() {
        streaming = false
        process?.let {
            if (it.isAlive) {
                it.destroy()
                Thread.sleep(1000)
                if (it.isAlive) it.destroyForcibly()
            }
        }
        process = null
        logThread = null
        logger.log(name, "Stopped")
    }

    override fun isStreaming() = streaming && process?.isAlive == true

    private object Toolkit {
        val screenSize: java.awt.Dimension
            get() = java.awt.Toolkit.getDefaultToolkit().screenSize
    }
}
