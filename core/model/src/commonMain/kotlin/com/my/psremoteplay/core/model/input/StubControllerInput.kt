package com.my.psremoteplay.core.model.input

class StubControllerInput : ControllerInputSender {
    override suspend fun connect(params: Map<String, String>) {}
    override suspend fun sendState(state: ControllerState) {}
    override suspend fun disconnect() {}
}
