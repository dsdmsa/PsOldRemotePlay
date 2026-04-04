package com.my.psremoteplay.core.streaming.input

interface ControllerInputSender {
    suspend fun connect(params: Map<String, String>)
    suspend fun sendState(state: ControllerState)
    suspend fun disconnect()
}
