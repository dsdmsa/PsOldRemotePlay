package com.my.psremoteplay.feature.ps2.strategy.server

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamServer
import java.io.File

/**
 * macOS ScreenCaptureKit + VideoToolbox H.264 server strategy.
 *
 * Pipeline: ScreenCaptureKit capture → VideoToolbox HW encode → H.264 Annex-B → UDP.
 *
 * Launches a native Swift helper (`tools/macos/screencap`) that handles the entire pipeline
 * in-process with hardware acceleration. Auto-compiles the helper from source on first use.
 *
 * Pairs with AndroidMediaCodecClient on the client side.
 */
class ScreenCaptureKitServer(private val logger: Logger) : VideoStreamServer {
    override val name = "H.264/NAL-UDP"

    @Volatile private var streaming = false
    private var process: Process? = null
    private var logThread: Thread? = null

    override fun start(config: StreamConfig) {
        if (streaming) return
        streaming = true

        val binary = ensureBinary()
        if (binary == null) {
            logger.error(name, "Failed to prepare screencap binary (macOS only)")
            streaming = false
            return
        }

        val audioPort = config.videoPort + 1
        val cmd = listOf(
            binary,
            "--target", config.targetIp,
            "--port", config.videoPort.toString(),
            "--audio-port", audioPort.toString(),
            "--width", config.width.toString(),
            "--height", config.height.toString(),
            "--fps", config.fps.toString(),
            "--bitrate", config.bitrate.toString(),
            "--keyframe", config.keyframeInterval.toString(),
            "--app", "PCSX2"
        )

        try {
            logger.log(name, "Starting ScreenCaptureKit + VideoToolbox (HW)")
            logger.log(name, "CMD: ${cmd.joinToString(" ")}")

            val pb = ProcessBuilder(cmd)
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
            pb.redirectError(ProcessBuilder.Redirect.PIPE)
            process = pb.start()

            logThread = Thread {
                process?.errorStream?.bufferedReader()?.forEachLine { line ->
                    if (streaming) logger.log("SCREENCAP", line)
                }
            }.apply { isDaemon = true; name = "screencap-log"; start() }

            logger.log(name, "H.264 HW stream → ${config.targetIp}:${config.videoPort}")
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

    private fun ensureBinary(): String? {
        // Resolve project root: walk up from CWD until we find settings.gradle.kts
        val projectRoot = generateSequence(File(".").canonicalFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: File(".").canonicalFile
        val source = File(projectRoot, "tools/macos/screencap.swift")
        val binary = File(projectRoot, "tools/macos/screencap")

        // Already compiled and up to date
        if (binary.exists() && binary.canExecute() &&
            (!source.exists() || binary.lastModified() >= source.lastModified())) {
            return binary.absolutePath
        }

        if (!source.exists()) {
            logger.error(name, "Source not found: ${source.absolutePath}")
            return null
        }

        logger.log(name, "Compiling screencap helper...")
        try {
            val compile = ProcessBuilder(
                "swiftc", "-O",
                "-o", binary.path,
                source.path,
                "-framework", "ScreenCaptureKit",
                "-framework", "VideoToolbox",
                "-framework", "CoreMedia",
                "-framework", "CoreVideo",
                "-framework", "CoreGraphics"
            ).redirectErrorStream(true).start()

            val output = compile.inputStream.bufferedReader().readText()
            val exitCode = compile.waitFor()
            if (exitCode != 0) {
                logger.error(name, "Compile failed ($exitCode): $output")
                return null
            }
            logger.log(name, "Compiled screencap helper")
            return binary.absolutePath
        } catch (e: Exception) {
            logger.error(name, "Compile error: ${e.message}", e)
            return null
        }
    }
}
