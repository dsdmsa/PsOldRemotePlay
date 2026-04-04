package com.my.psremoteplay.feature.ps3.di

import com.my.psremoteplay.core.streaming.Crypto
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.StreamingDependencies
import com.my.psremoteplay.core.streaming.audio.AudioRenderer
import com.my.psremoteplay.core.streaming.codec.AudioDecoder
import com.my.psremoteplay.core.streaming.codec.VideoDecoder
import com.my.psremoteplay.core.streaming.input.ControllerInputSender
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.core.streaming.stubs.LoggingVideoRenderer
import com.my.psremoteplay.core.streaming.stubs.StubAudioDecoder
import com.my.psremoteplay.core.streaming.stubs.StubAudioRenderer
import com.my.psremoteplay.core.streaming.stubs.StubVideoDecoder
import com.my.psremoteplay.core.streaming.upscale.PassthroughUpscaler
import com.my.psremoteplay.core.streaming.upscale.UpscaleFilter
import com.my.psremoteplay.core.streaming.video.VideoRenderer
import com.my.psremoteplay.feature.ps3.protocol.PremoRegistration
import com.my.psremoteplay.feature.ps3.protocol.PremoSessionHandler
import com.my.psremoteplay.feature.ps3.protocol.Ps3Discoverer
import com.my.psremoteplay.feature.ps3.protocol.Ps3Info
import com.my.psremoteplay.feature.ps3.protocol.RegistrationResult
import com.my.psremoteplay.feature.ps3.protocol.SessionConfig
import com.my.psremoteplay.feature.ps3.protocol.SessionResponse
import com.my.psremoteplay.feature.ps3.protocol.StreamPacket

class IosPs3Dependencies : Ps3Dependencies {

    private val logger: Logger = object : Logger {
        override fun log(tag: String, message: String) {
            println("[$tag] $message")
        }
        override fun error(tag: String, message: String, throwable: Throwable?) {
            println("[$tag] ERROR: $message${throwable?.let { " - ${it.message}" } ?: ""}")
        }
    }

    override val streaming: StreamingDependencies = object : StreamingDependencies {
        override val crypto: Crypto = object : Crypto {
            override fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
                error("Not implemented")
            override fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray =
                error("Not implemented")
            override fun base64Encode(data: ByteArray): String =
                error("Not implemented")
            override fun base64Decode(data: String): ByteArray =
                error("Not implemented")
            override fun randomBytes(count: Int): ByteArray =
                error("Not implemented")
        }
        override val videoDecoder: VideoDecoder = StubVideoDecoder()
        override val audioDecoder: AudioDecoder = StubAudioDecoder()
        override val videoRenderer: VideoRenderer = LoggingVideoRenderer(logger)
        override val audioRenderer: AudioRenderer = StubAudioRenderer(logger)
        override val upscaleFilter: UpscaleFilter = PassthroughUpscaler()
        override val logger: Logger = this@IosPs3Dependencies.logger
    }

    override val discoverer: Ps3Discoverer = object : Ps3Discoverer {
        override suspend fun discover(timeoutMs: Int): Ps3Info? =
            error("Not implemented")
        override suspend fun discoverDirect(ip: String, timeoutMs: Int): Ps3Info? =
            error("Not implemented")
    }

    override val sessionHandler: PremoSessionHandler = object : PremoSessionHandler {
        override suspend fun createSession(ps3Ip: String, config: SessionConfig): Result<SessionResponse> =
            error("Not implemented")
        override suspend fun startVideoStream(
            ps3Ip: String, sessionId: String, authToken: String,
            aesKey: ByteArray, aesIv: ByteArray, onPacket: suspend (StreamPacket) -> Unit
        ) = error("Not implemented")
        override suspend fun startAudioStream(
            ps3Ip: String, sessionId: String, authToken: String,
            aesKey: ByteArray, aesIv: ByteArray, onPacket: suspend (StreamPacket) -> Unit
        ) = error("Not implemented")
        override fun disconnect() = error("Not implemented")
    }

    override val registration: PremoRegistration = object : PremoRegistration {
        override suspend fun register(
            ps3Ip: String, pin: String, deviceId: ByteArray, deviceMac: ByteArray,
            deviceName: String, platformType: Int
        ): Result<RegistrationResult> = error("Not implemented")
    }

    override val controllerInput: ControllerInputSender = object : ControllerInputSender {
        override suspend fun connect(params: Map<String, String>) = error("Not implemented")
        override suspend fun sendState(state: ControllerState) = error("Not implemented")
        override suspend fun disconnect() = error("Not implemented")
    }
}
