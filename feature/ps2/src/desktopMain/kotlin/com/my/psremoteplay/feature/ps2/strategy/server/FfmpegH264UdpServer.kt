package com.my.psremoteplay.feature.ps2.strategy.server

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamServer

/**
 * High-performance H.264 server strategy.
 *
 * Pipeline: FFmpeg avfoundation capture → libx264 ultrafast/zerolatency → H.264 via UDP.
 *
 * Requires macOS Screen Recording permission for FFmpeg.
 * Grant via: System Settings → Privacy & Security → Screen Recording → add ffmpeg binary.
 *
 * Pairs with AndroidMediaCodecClient on the client side.
 */
class FfmpegH264UdpServer(private val logger: Logger) : VideoStreamServer {
    override val name = "H.264/NAL-UDP"

    @Volatile private var streaming = false
    private var process: Process? = null
    private var logThread: Thread? = null

    override fun start(config: StreamConfig) {
        if (streaming) return
        streaming = true

        val os = System.getProperty("os.name").lowercase()
        val captureArgs = buildCaptureArgs(os, config)
        if (captureArgs == null) {
            logger.error(name, "Unsupported OS: $os")
            streaming = false
            return
        }

        val encodeArgs = buildEncodeArgs(config)
        val udpUrl = "udp://${config.targetIp}:${config.videoPort}?pkt_size=32768"

        val cmd = listOf("ffmpeg", "-y") + captureArgs + encodeArgs + listOf(
            "-f", "h264",
            udpUrl
        )

        try {
            logger.log(name, "Starting FFmpeg (libx264)")
            logger.log(name, "CMD: ${cmd.joinToString(" ")}")

            val pb = ProcessBuilder(cmd)
            pb.redirectError(ProcessBuilder.Redirect.PIPE)
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
            process = pb.start()

            logThread = Thread {
                process?.errorStream?.bufferedReader()?.forEachLine { line ->
                    if (streaming) logger.log("FFMPEG", line)
                }
            }.apply { isDaemon = true; name = "ffmpeg-log"; start() }

            logger.log(name, "H.264 stream → ${config.targetIp}:${config.videoPort}")
        } catch (e: Exception) {
            logger.error(name, "Failed to start: ${e.message}", e)
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

    private fun buildCaptureArgs(os: String, config: StreamConfig): List<String>? = when {
        os.contains("mac") -> listOf(
            "-f", "avfoundation",
            "-framerate", config.fps.toString(),
            "-pixel_format", "nv12",
            "-capture_cursor", "0",
            "-i", "1:none"
        )
        os.contains("linux") -> listOf(
            "-f", "x11grab",
            "-framerate", config.fps.toString(),
            "-video_size", "${java.awt.Toolkit.getDefaultToolkit().screenSize.let { "${it.width}x${it.height}" }}",
            "-i", ":0.0"
        )
        else -> null
    }

    private fun buildEncodeArgs(config: StreamConfig): List<String> = listOf(
        "-fps_mode", "cfr",
        "-vf", "scale=${config.width}:${config.height}",
        "-c:v", "libx264",
        "-preset", "ultrafast",
        "-tune", "zerolatency",
        "-profile:v", config.profile,
        "-b:v", config.bitrate.toString(),
        "-maxrate", (config.bitrate * 1.2).toInt().toString(),
        "-bufsize", (config.bitrate / 5).toString(),
        "-g", config.keyframeInterval.toString(),
        "-bf", "0",
        "-refs", "1",
        "-rc-lookahead", "0"
    )
}
