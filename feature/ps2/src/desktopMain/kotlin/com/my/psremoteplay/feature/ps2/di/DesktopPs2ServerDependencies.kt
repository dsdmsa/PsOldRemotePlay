package com.my.psremoteplay.feature.ps2.di

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.feature.ps2.platform.*
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.StreamingPreset
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamServer
import com.my.psremoteplay.feature.ps2.strategy.server.*

class DesktopPs2ServerDependencies(
    preset: StreamingPreset = StreamingPreset.JPEG_UDP
) : Ps2ServerDependencies {

    /**
     * Set a listener to receive ALL log messages (including from subsystems like FFmpeg,
     * control server, etc.) in the UI. Called from ViewModel to pipe logs to the log panel.
     */
    var logListener: ((tag: String, message: String, isError: Boolean) -> Unit)? = null

    override val logger: Logger = object : Logger {
        override fun log(tag: String, message: String) {
            println("[$tag] $message")
            logListener?.invoke(tag, message, false)
        }
        override fun error(tag: String, message: String, throwable: Throwable?) {
            val msg = "$message${throwable?.let { "\n  ${it::class.simpleName}: ${it.message}" } ?: ""}"
            System.err.println("[ERROR:$tag] $msg")
            logListener?.invoke(tag, msg, true)
        }
    }

    override fun installLogListener(listener: (String, String, Boolean) -> Unit) {
        logListener = listener
    }

    override val streamConfig = StreamConfig()

    /** ← Change this to swap the entire video pipeline */
    override val videoStreamServer: VideoStreamServer = createServerStrategy(preset)

    private val pcsx2Launcher = Pcsx2Launcher(logger)
    private val keyInjector = KeyInjector(logger)
    private val controlServer = Ps2ControlServer(logger)

    private fun createServerStrategy(preset: StreamingPreset): VideoStreamServer = when (preset) {
        StreamingPreset.JPEG_TCP -> RobotJpegTcpServer(logger)
        StreamingPreset.JPEG_UDP -> RobotJpegUdpServer(logger)
        StreamingPreset.H264_RTP -> JavaCvRtpServer(logger)
        StreamingPreset.H264_MPEGTS -> FfmpegMpegTsServer(logger)
        StreamingPreset.PCSX2_PIPE -> Pcsx2PipeServer(logger)
        StreamingPreset.H264_HW -> ScreenCaptureKitServer(logger)
    }

    override fun launchEmulator(emulatorPath: String, gamePath: String): Boolean =
        pcsx2Launcher.launch(emulatorPath, gamePath)
    override fun stopEmulator() = pcsx2Launcher.stop()
    override fun isEmulatorRunning(): Boolean = pcsx2Launcher.isRunning()

    override fun startControlServer(port: Int) = controlServer.start(port)
    override fun stopControlServer() = controlServer.stop()
    override fun isControlServerRunning(): Boolean = controlServer.isRunning()
    override fun getClientCount(): Int = controlServer.getClientCount()
    override fun getLastClientIp(): String? = controlServer.getLastClientIp()
    override fun onClientInput(handler: (ControllerState) -> Unit) = controlServer.onClientInput(handler)

    override fun injectButtonPress(buttonMask: Int) {
        val keyCode = ButtonToKeyMap.mapping[buttonMask] ?: return
        keyInjector.pressKey(keyCode)
    }
    override fun injectButtonRelease(buttonMask: Int) {
        val keyCode = ButtonToKeyMap.mapping[buttonMask] ?: return
        keyInjector.releaseKey(keyCode)
    }
    override fun injectStickState(state: ControllerState) {
        val threshold = 0.3f
        injectStickAxis(state.leftStickY > threshold, ButtonToKeyMap.leftStickUp)
        injectStickAxis(state.leftStickY < -threshold, ButtonToKeyMap.leftStickDown)
        injectStickAxis(state.leftStickX < -threshold, ButtonToKeyMap.leftStickLeft)
        injectStickAxis(state.leftStickX > threshold, ButtonToKeyMap.leftStickRight)
        injectStickAxis(state.rightStickY > threshold, ButtonToKeyMap.rightStickUp)
        injectStickAxis(state.rightStickY < -threshold, ButtonToKeyMap.rightStickDown)
        injectStickAxis(state.rightStickX < -threshold, ButtonToKeyMap.rightStickLeft)
        injectStickAxis(state.rightStickX > threshold, ButtonToKeyMap.rightStickRight)
    }

    private val stickKeyState = mutableMapOf<Int, Boolean>()
    private fun injectStickAxis(active: Boolean, keyCode: Int) {
        val wasActive = stickKeyState[keyCode] == true
        if (active && !wasActive) { keyInjector.pressKey(keyCode); stickKeyState[keyCode] = true }
        else if (!active && wasActive) { keyInjector.releaseKey(keyCode); stickKeyState[keyCode] = false }
    }
}
