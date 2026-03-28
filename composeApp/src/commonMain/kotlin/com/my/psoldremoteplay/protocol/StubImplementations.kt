package com.my.psoldremoteplay.protocol

class StubRegistration : PremoRegistration {
    override suspend fun register(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        platformType: Int
    ): Result<RegistrationResult> {
        return Result.failure(
            UnsupportedOperationException(
                "Registration encryption not yet solved. " +
                "Use HEN PS3 with pre-extracted keys or wait for community breakthrough."
            )
        )
    }
}

class StubControllerInput : ControllerInputSender {
    override suspend fun connect(ps3Ip: String, sessionId: String, authToken: String) {}
    override suspend fun sendState(state: ControllerState) {}
    override fun disconnect() {}
}

class LoggingVideoRenderer(private val logger: PremoLogger) : VideoRenderer {
    private var packetCount = 0

    override fun start() {
        logger.log("VIDEO", "Video renderer started")
    }

    override fun onStreamPacket(header: ByteArray, payload: ByteArray, isEncrypted: Boolean) {
        packetCount++
        if (packetCount % 30 == 0) {
            logger.log("VIDEO", "Received $packetCount packets, last payload: ${payload.size} bytes")
        }
    }

    override fun stop() {
        logger.log("VIDEO", "Video renderer stopped ($packetCount total packets)")
        packetCount = 0
    }
}
