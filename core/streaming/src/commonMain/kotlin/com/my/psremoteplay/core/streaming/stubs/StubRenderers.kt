package com.my.psremoteplay.core.streaming.stubs

import androidx.compose.ui.graphics.ImageBitmap
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.audio.AudioRenderer
import com.my.psremoteplay.core.streaming.codec.AudioBuffer
import com.my.psremoteplay.core.streaming.input.ControllerInputSender
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.core.streaming.video.VideoRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoggingVideoRenderer(private val logger: Logger) : VideoRenderer {
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

class StubAudioRenderer(private val logger: Logger) : AudioRenderer {
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

class StubControllerInput : ControllerInputSender {
    override suspend fun connect(params: Map<String, String>) {}
    override suspend fun sendState(state: ControllerState) {}
    override suspend fun disconnect() {}
}
