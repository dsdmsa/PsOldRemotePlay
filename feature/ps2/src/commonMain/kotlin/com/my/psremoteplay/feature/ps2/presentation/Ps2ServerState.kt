package com.my.psremoteplay.feature.ps2.presentation

import com.my.psremoteplay.core.streaming.LogEntry

data class Ps2ServerState(
    val pcsx2Path: String = "/Users/mihailurmanschi/Desktop/PS2/PCSX2-v2.7.230.app/Contents/MacOS/PCSX2",
    val gamePath: String = "/Users/mihailurmanschi/Desktop/PS2/GAMES/Batman Begins (USA).chd",
    val serverPort: Int = 9295,
    val isServerRunning: Boolean = false,
    val isPcsx2Running: Boolean = false,
    val isCapturing: Boolean = false,
    val connectedClients: Int = 0,
    val framesSent: Long = 0,
    val inputsReceived: Long = 0,
    val logs: List<LogEntry> = emptyList(),
    val statusText: String = "Ready"
)

sealed interface Ps2ServerIntent {
    data class UpdatePcsx2Path(val path: String) : Ps2ServerIntent
    data class UpdateGamePath(val path: String) : Ps2ServerIntent
    data class UpdatePort(val port: Int) : Ps2ServerIntent
    data object StartAll : Ps2ServerIntent
    data object StopAll : Ps2ServerIntent
    data object ClearLogs : Ps2ServerIntent
    data object CopyLogs : Ps2ServerIntent
}

sealed interface Ps2ServerEffect {
    data class CopyToClipboard(val text: String) : Ps2ServerEffect
    data class ShowMessage(val message: String) : Ps2ServerEffect
}
