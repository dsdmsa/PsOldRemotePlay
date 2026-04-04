package com.my.psremoteplay.core.streaming

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
