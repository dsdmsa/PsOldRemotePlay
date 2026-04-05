package com.my.psremoteplay.feature.ps2.di

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamServer

interface Ps2ServerDependencies {
    val logger: Logger

    /** Install a listener to receive all log messages in the UI */
    fun installLogListener(listener: (tag: String, message: String, isError: Boolean) -> Unit) {}

    /** Active video stream strategy — swap to change the entire pipeline */
    val videoStreamServer: VideoStreamServer

    /** Default stream configuration */
    val streamConfig: StreamConfig

    // Emulator control
    fun launchEmulator(emulatorPath: String, gamePath: String): Boolean
    fun stopEmulator()
    fun isEmulatorRunning(): Boolean

    // Control channel (TCP): server info, controller input
    fun startControlServer(port: Int)
    fun stopControlServer()
    fun isControlServerRunning(): Boolean
    fun getClientCount(): Int
    fun getLastClientIp(): String?
    fun onClientInput(handler: (ControllerState) -> Unit)

    // Input injection into emulator
    fun injectButtonPress(buttonMask: Int)
    fun injectButtonRelease(buttonMask: Int)
    fun injectStickState(state: ControllerState)
}
