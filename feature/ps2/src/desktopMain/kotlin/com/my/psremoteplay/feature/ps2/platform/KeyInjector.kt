package com.my.psremoteplay.feature.ps2.platform

import com.my.psremoteplay.core.streaming.Logger
import java.io.File
import java.io.OutputStreamWriter
import java.awt.event.KeyEvent as AwtKeyEvent

/**
 * Injects keyboard input into PCSX2 using macOS CGEvents via a native helper.
 *
 * Java Robot sends events to the focused window's event queue, but PCSX2 (SDL2)
 * reads input at a lower level and doesn't receive Robot events.
 *
 * This class launches a native Swift helper (`tools/macos/keyinject`) that stays
 * running and posts CGEvents via stdin commands. CGEvents reach PCSX2 regardless
 * of window focus because they're posted to kCGHIDEventTap.
 */
class KeyInjector(private val logger: Logger) {

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private val pressedKeys = mutableSetOf<Int>()
    private var keyPressCount = 0L

    init {
        startHelper()
    }

    /** Set the target PID and activate the app so it receives CGEvents. */
    fun setTargetPid(pid: Long) {
        sendCommand("P $pid")
        logger.log("INPUT", "keyinject targeting PID $pid")
        // Ensure PCSX2 stays in front — re-activate after a short delay
        // (PCSX2 may take a moment to create its window)
        Thread {
            Thread.sleep(3000)
            sendCommand("F")
            logger.log("INPUT", "Re-activated PCSX2 window")
        }.apply { isDaemon = true; start() }
    }

    fun pressKey(keyCode: Int) {
        val cgKey = awtToCGKeyCode(keyCode)
        if (cgKey < 0) return
        synchronized(pressedKeys) {
            if (!pressedKeys.add(cgKey)) return
        }
        sendCommand("D $cgKey")
        keyPressCount++
        if (keyPressCount <= 5 || keyPressCount % 50 == 0L) {
            val keyName = AwtKeyEvent.getKeyText(keyCode)
            logger.log("INPUT", "CGEvent keyDown($keyName cg=$cgKey) #$keyPressCount")
        }
    }

    fun releaseKey(keyCode: Int) {
        val cgKey = awtToCGKeyCode(keyCode)
        if (cgKey < 0) return
        synchronized(pressedKeys) {
            if (!pressedKeys.remove(cgKey)) return
        }
        sendCommand("U $cgKey")
    }

    fun stop() {
        sendCommand("Q")
        process?.let {
            it.destroy()
            Thread.sleep(500)
            if (it.isAlive) it.destroyForcibly()
        }
        process = null
        writer = null
    }

    private fun sendCommand(cmd: String) {
        try {
            writer?.write("$cmd\n")
            writer?.flush()
        } catch (e: Exception) {
            logger.error("INPUT", "keyinject write failed: ${e.message}")
            // Try to restart
            startHelper()
        }
    }

    private fun startHelper() {
        val binary = ensureBinary() ?: return
        try {
            val pb = ProcessBuilder(binary)
            pb.redirectError(ProcessBuilder.Redirect.PIPE)
            val proc = pb.start()
            process = proc
            writer = OutputStreamWriter(proc.outputStream)

            // Read stderr for "OK" confirmation or errors
            Thread {
                proc.errorStream.bufferedReader().forEachLine { line ->
                    if (line.startsWith("ERROR")) {
                        logger.error("INPUT", "keyinject: $line")
                    } else {
                        logger.log("INPUT", "keyinject: $line")
                    }
                }
            }.apply { isDaemon = true; name = "keyinject-log"; start() }

            logger.log("INPUT", "CGEvent keyinject helper started")
        } catch (e: Exception) {
            logger.error("INPUT", "Failed to start keyinject: ${e.message}", e)
        }
    }

    private fun ensureBinary(): String? {
        val projectRoot = generateSequence(File(".").canonicalFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: File(".").canonicalFile
        val source = File(projectRoot, "tools/macos/keyinject.swift")
        val binary = File(projectRoot, "tools/macos/keyinject")

        if (binary.exists() && binary.canExecute() &&
            (!source.exists() || binary.lastModified() >= source.lastModified())) {
            return binary.absolutePath
        }

        if (!source.exists()) {
            logger.error("INPUT", "keyinject source not found: ${source.absolutePath}")
            return null
        }

        logger.log("INPUT", "Compiling keyinject helper...")
        try {
            val compile = ProcessBuilder(
                "swiftc", "-O", "-o", binary.path, source.path,
                "-framework", "CoreGraphics", "-framework", "AppKit"
            ).redirectErrorStream(true).start()

            val output = compile.inputStream.bufferedReader().readText()
            val exitCode = compile.waitFor()
            if (exitCode != 0) {
                logger.error("INPUT", "keyinject compile failed ($exitCode): $output")
                return null
            }
            logger.log("INPUT", "Compiled keyinject helper")
            return binary.absolutePath
        } catch (e: Exception) {
            logger.error("INPUT", "keyinject compile error: ${e.message}", e)
            return null
        }
    }

    /** Maps Java AWT VK_ codes to macOS CGKeyCode values. */
    private fun awtToCGKeyCode(awtKeyCode: Int): Int = when (awtKeyCode) {
        AwtKeyEvent.VK_A -> 0x00;  AwtKeyEvent.VK_S -> 0x01
        AwtKeyEvent.VK_D -> 0x02;  AwtKeyEvent.VK_F -> 0x03
        AwtKeyEvent.VK_H -> 0x04;  AwtKeyEvent.VK_G -> 0x05
        AwtKeyEvent.VK_Z -> 0x06;  AwtKeyEvent.VK_X -> 0x07
        AwtKeyEvent.VK_C -> 0x08;  AwtKeyEvent.VK_V -> 0x09
        AwtKeyEvent.VK_B -> 0x0B;  AwtKeyEvent.VK_Q -> 0x0C
        AwtKeyEvent.VK_W -> 0x0D;  AwtKeyEvent.VK_E -> 0x0E
        AwtKeyEvent.VK_R -> 0x0F;  AwtKeyEvent.VK_T -> 0x11
        AwtKeyEvent.VK_Y -> 0x10;  AwtKeyEvent.VK_U -> 0x20
        AwtKeyEvent.VK_I -> 0x22;  AwtKeyEvent.VK_O -> 0x1F
        AwtKeyEvent.VK_P -> 0x23;  AwtKeyEvent.VK_L -> 0x25
        AwtKeyEvent.VK_J -> 0x26;  AwtKeyEvent.VK_K -> 0x28
        AwtKeyEvent.VK_N -> 0x2D;  AwtKeyEvent.VK_M -> 0x2E
        AwtKeyEvent.VK_1 -> 0x12;  AwtKeyEvent.VK_2 -> 0x13
        AwtKeyEvent.VK_3 -> 0x14;  AwtKeyEvent.VK_4 -> 0x15
        AwtKeyEvent.VK_5 -> 0x17;  AwtKeyEvent.VK_6 -> 0x16
        AwtKeyEvent.VK_7 -> 0x1A;  AwtKeyEvent.VK_8 -> 0x1C
        AwtKeyEvent.VK_9 -> 0x19;  AwtKeyEvent.VK_0 -> 0x1D
        AwtKeyEvent.VK_ENTER -> 0x24;      AwtKeyEvent.VK_BACK_SPACE -> 0x33
        AwtKeyEvent.VK_SPACE -> 0x31;      AwtKeyEvent.VK_ESCAPE -> 0x35
        AwtKeyEvent.VK_TAB -> 0x30;        AwtKeyEvent.VK_SHIFT -> 0x38
        AwtKeyEvent.VK_UP -> 0x7E;         AwtKeyEvent.VK_DOWN -> 0x7D
        AwtKeyEvent.VK_LEFT -> 0x7B;       AwtKeyEvent.VK_RIGHT -> 0x7C
        AwtKeyEvent.VK_F1 -> 0x7A;         AwtKeyEvent.VK_F2 -> 0x78
        AwtKeyEvent.VK_F3 -> 0x63;         AwtKeyEvent.VK_F4 -> 0x76
        else -> -1
    }
}
