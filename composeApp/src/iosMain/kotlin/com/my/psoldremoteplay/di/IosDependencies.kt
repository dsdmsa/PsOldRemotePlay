package com.my.psoldremoteplay.di

import com.my.psoldremoteplay.*
import com.my.psoldremoteplay.protocol.*
import com.my.psoldremoteplay.protocol.codec.AudioDecoder
import com.my.psoldremoteplay.protocol.codec.VideoDecoder
import com.my.psoldremoteplay.protocol.upscale.PassthroughUpscaler
import com.my.psoldremoteplay.protocol.upscale.UpscaleFilter

/**
 * iOS dependencies (stub implementation).
 * iOS platform is not yet fully functional — network stack needs native Swift integration.
 */
class IosDependencies : PlatformDependencies {
    override val logger: PremoLogger = object : PremoLogger {
        override fun log(tag: String, message: String) {
            println("[$tag] $message")
        }
        override fun error(tag: String, message: String, throwable: Throwable?) {
            System.err.println("[ERROR:$tag] $message")
            throwable?.printStackTrace(System.err)
        }
    }

    override val crypto: PremoCrypto = object : PremoCrypto {
        override fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
            error("iOS crypto not implemented yet")
        }
        override fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
            error("iOS crypto not implemented yet")
        }
        override fun base64Encode(data: ByteArray): String {
            error("iOS crypto not implemented yet")
        }
        override fun base64Decode(data: String): ByteArray {
            error("iOS crypto not implemented yet")
        }
        override fun randomBytes(length: Int): ByteArray {
            error("iOS crypto not implemented yet")
        }
    }

    override val discoverer: Ps3Discoverer = object : Ps3Discoverer {
        override suspend fun discover(timeoutMs: Int): Ps3Info? {
            error("iOS networking not implemented yet")
        }
        override suspend fun discoverDirect(ip: String, timeoutMs: Int): Ps3Info? {
            error("iOS networking not implemented yet")
        }
    }

    override val sessionHandler: PremoSessionHandler = object : PremoSessionHandler {
        override suspend fun createSession(ps3Ip: String, config: SessionConfig) =
            Result.failure<SessionResponse>(UnsupportedOperationException("iOS networking not implemented"))
        override suspend fun startVideoStream(ps3Ip: String, sessionId: String, authToken: String, aesKey: ByteArray, aesIv: ByteArray, onPacket: (StreamPacket) -> Unit) =
            error("iOS networking not implemented")
        override suspend fun startAudioStream(ps3Ip: String, sessionId: String, authToken: String, aesKey: ByteArray, aesIv: ByteArray, onPacket: (StreamPacket) -> Unit) =
            error("iOS networking not implemented")
        override fun disconnect() {}
    }

    override val registration: PremoRegistration = StubRegistration()
    override val videoDecoder: VideoDecoder = StubVideoDecoder()
    override val audioDecoder: AudioDecoder = StubAudioDecoder()
    override val videoRenderer: VideoRenderer = LoggingVideoRenderer(logger)
    override val audioRenderer: AudioRenderer = StubAudioRenderer(logger)
    override val upscaleFilter: UpscaleFilter = PassthroughUpscaler()
    override val controllerInput: ControllerInputSender = StubControllerInput()
}
