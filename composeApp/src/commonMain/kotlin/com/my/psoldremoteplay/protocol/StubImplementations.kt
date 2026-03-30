package com.my.psoldremoteplay.protocol

import androidx.compose.ui.graphics.ImageBitmap
import com.my.psoldremoteplay.protocol.codec.AudioBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    override suspend fun disconnect() {}
}

class LoggingVideoRenderer(private val logger: PremoLogger) : VideoRenderer {
    private var packetCount = 0
    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    override val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    override suspend fun start() {
        logger.log("VIDEO", "Video renderer started")
    }

    override suspend fun onStreamPacket(header: ByteArray, payload: ByteArray, isEncrypted: Boolean) {
        packetCount++
        if (packetCount % 30 == 0) {
            logger.log("VIDEO", "Received $packetCount packets, last payload: ${payload.size} bytes")
        }
    }

    override suspend fun stop() {
        logger.log("VIDEO", "Video renderer stopped ($packetCount total packets)")
        packetCount = 0
        _currentFrame.value = null
    }
}

class StubAudioRenderer(private val logger: PremoLogger) : AudioRenderer {
    override suspend fun start() {
        logger.log("AUDIO", "Audio renderer started")
    }

    override suspend fun onAudioBuffer(buffer: AudioBuffer) {
        logger.log("AUDIO", "Audio buffer: ${buffer.samples.size} bytes @ ${buffer.sampleRate}Hz")
    }

    override suspend fun stop() {
        logger.log("AUDIO", "Audio renderer stopped")
    }
}
