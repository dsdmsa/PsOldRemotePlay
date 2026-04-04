package com.my.psremoteplay.feature.ps3.presentation

import com.my.psremoteplay.core.streaming.ConnectionStatus
import com.my.psremoteplay.core.streaming.LogEntry

data class Ps3State(
    val ps3Ip: String = "",
    val ps3Nickname: String = "",
    val ps3Mac: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val pkey: String = "",
    val deviceId: String = "00112233445566778899AABBCCDDEEFF",
    val deviceMac: String = "001122334455",
    val pin: String = "",
    val sessionId: String = "",
    val videoPacketCount: Int = 0,
    val logs: List<LogEntry> = emptyList(),
    val isStreaming: Boolean = false,
    val statusText: String = "Ready"
)

sealed interface Ps3Intent {
    data object DiscoverBroadcast : Ps3Intent
    data class DiscoverDirect(val ip: String) : Ps3Intent
    data class UpdateIp(val ip: String) : Ps3Intent
    data class UpdatePkey(val pkey: String) : Ps3Intent
    data class UpdatePin(val pin: String) : Ps3Intent
    data class UpdateDeviceId(val id: String) : Ps3Intent
    data class UpdateDeviceMac(val mac: String) : Ps3Intent
    data object ConnectSession : Ps3Intent
    data object TestConnection : Ps3Intent
    data object Register : Ps3Intent
    data object Disconnect : Ps3Intent
    data object ClearLogs : Ps3Intent
    data object CopyLogs : Ps3Intent
}

sealed interface Ps3Effect {
    data class CopyToClipboard(val text: String) : Ps3Effect
    data class ShowMessage(val message: String) : Ps3Effect
}
