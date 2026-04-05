package com.my.psremoteplay.feature.ps2.platform

import com.my.psremoteplay.core.streaming.Logger

class Pcsx2Launcher(private val logger: Logger) {
    private var process: Process? = null

    fun launch(pcsx2Path: String, gamePath: String): Boolean {
        if (process?.isAlive == true) {
            logger.log("PCSX2", "Already running")
            return true
        }
        return try {
            logger.log("PCSX2", "Launching: $pcsx2Path")
            logger.log("PCSX2", "Game: $gamePath")
            val pb = ProcessBuilder(pcsx2Path, "-batch", gamePath)
            pb.redirectErrorStream(true)
            process = pb.start()
            // Read output in background thread to prevent blocking
            Thread {
                process?.inputStream?.bufferedReader()?.forEachLine {
                    logger.log("PCSX2", it)
                }
            }.apply { isDaemon = true; start() }
            logger.log("PCSX2", "Process started (PID: ${process?.pid()})")
            true
        } catch (e: Exception) {
            logger.error("PCSX2", "Failed to launch: ${e.message}", e)
            false
        }
    }

    fun stop() {
        process?.let {
            if (it.isAlive) {
                logger.log("PCSX2", "Stopping PCSX2...")
                it.destroy()
                Thread.sleep(2000)
                if (it.isAlive) it.destroyForcibly()
                logger.log("PCSX2", "PCSX2 stopped")
            }
        }
        process = null
    }

    fun isRunning(): Boolean = process?.isAlive == true
    fun getPid(): Long = process?.pid() ?: -1
}
