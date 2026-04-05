package com.my.psremoteplay.core.model.input

interface ControllerInputSender {
    suspend fun connect(params: Map<String, String>)
    suspend fun sendState(state: ControllerState)
    suspend fun disconnect()
}
