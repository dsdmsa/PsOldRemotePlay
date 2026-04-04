package com.my.psremoteplay.feature.ps2.presentation

import com.my.psremoteplay.core.streaming.ConnectionStatus
import com.my.psremoteplay.core.streaming.LogEntry
import com.my.psremoteplay.core.streaming.input.ControllerState

data class Ps2ClientState(
    val serverIp: String = "localhost",
    val serverPort: Int = 9295,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val gameName: String = "",
    val videoFrameCount: Int = 0,
    val logs: List<LogEntry> = emptyList(),
    val isStreaming: Boolean = false,
    val showController: Boolean = true,
    val statusText: String = "Ready"
)

sealed interface Ps2ClientIntent {
    data class UpdateServerIp(val ip: String) : Ps2ClientIntent
    data class UpdateServerPort(val port: Int) : Ps2ClientIntent
    data object Connect : Ps2ClientIntent
    data object Disconnect : Ps2ClientIntent
    data class ControllerInput(val state: ControllerState) : Ps2ClientIntent
    data object ToggleController : Ps2ClientIntent
    data object ClearLogs : Ps2ClientIntent
}

sealed interface Ps2ClientEffect {
    data class ShowMessage(val message: String) : Ps2ClientEffect
}
