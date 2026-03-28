package com.my.psoldremoteplay.presentation

data class LogEntry(
    val timestamp: String,
    val tag: String,
    val message: String,
    val isError: Boolean = false
)

enum class ConnectionStatus {
    Disconnected,
    Discovering,
    Connecting,
    Connected,
    Streaming,
    Error
}

data class RemotePlayState(
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

sealed interface RemotePlayIntent {
    data object DiscoverBroadcast : RemotePlayIntent
    data class DiscoverDirect(val ip: String) : RemotePlayIntent
    data class UpdateIp(val ip: String) : RemotePlayIntent
    data class UpdatePkey(val pkey: String) : RemotePlayIntent
    data class UpdatePin(val pin: String) : RemotePlayIntent
    data class UpdateDeviceId(val id: String) : RemotePlayIntent
    data class UpdateDeviceMac(val mac: String) : RemotePlayIntent
    data object ConnectSession : RemotePlayIntent
    data object TestConnection : RemotePlayIntent
    data object Register : RemotePlayIntent
    data object Disconnect : RemotePlayIntent
    data object ClearLogs : RemotePlayIntent
    data object CopyLogs : RemotePlayIntent
}

sealed interface RemotePlayEffect {
    data class CopyToClipboard(val text: String) : RemotePlayEffect
    data class ShowMessage(val message: String) : RemotePlayEffect
}
