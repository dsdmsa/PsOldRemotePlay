package com.my.psremoteplay.feature.ps2.di

import androidx.compose.ui.graphics.ImageBitmap
import com.my.psremoteplay.core.streaming.Crypto
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.StreamingDependencies
import com.my.psremoteplay.core.streaming.audio.AudioRenderer
import com.my.psremoteplay.core.streaming.codec.AudioDecoder
import com.my.psremoteplay.core.streaming.codec.VideoDecoder
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.core.streaming.stubs.LoggingVideoRenderer
import com.my.psremoteplay.core.streaming.stubs.StubAudioDecoder
import com.my.psremoteplay.core.streaming.stubs.StubAudioRenderer
import com.my.psremoteplay.core.streaming.stubs.StubVideoDecoder
import com.my.psremoteplay.core.streaming.upscale.PassthroughUpscaler
import com.my.psremoteplay.core.streaming.upscale.UpscaleFilter
import com.my.psremoteplay.core.streaming.video.VideoRenderer
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IosPs2ClientDependencies : Ps2ClientDependencies {
    private val _logger: Logger = object : Logger {
        override fun log(tag: String, message: String) { println("[$tag] $message") }
        override fun error(tag: String, message: String, throwable: Throwable?) {
            println("[$tag] ERROR: $message${throwable?.let { " - ${it.message}" } ?: ""}")
        }
    }

    override val streaming: StreamingDependencies = object : StreamingDependencies {
        override val crypto: Crypto = object : Crypto {
            override fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray = error("Not implemented")
            override fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray = error("Not implemented")
            override fun base64Encode(data: ByteArray): String = error("Not implemented")
            override fun base64Decode(data: String): ByteArray = error("Not implemented")
            override fun randomBytes(count: Int): ByteArray = error("Not implemented")
        }
        override val videoDecoder: VideoDecoder = StubVideoDecoder()
        override val audioDecoder: AudioDecoder = StubAudioDecoder()
        override val videoRenderer: VideoRenderer = LoggingVideoRenderer(_logger)
        override val audioRenderer: AudioRenderer = StubAudioRenderer(_logger)
        override val upscaleFilter: UpscaleFilter = PassthroughUpscaler()
        override val logger: Logger = _logger
    }

    override val logger: Logger = _logger
    override val streamConfig = StreamConfig()

    override val videoStreamClient: VideoStreamClient = object : VideoStreamClient {
        override val name = "Stub"
        override val currentFrame: StateFlow<ImageBitmap?> = MutableStateFlow(null)
        override fun start(serverIp: String, config: StreamConfig) = false
        override fun stop() {}
        override fun isReceiving() = false
    }

    override fun connectControl(ip: String, port: Int, onServerInfo: (String) -> Unit) = false
    override fun disconnectControl() {}
    override fun isConnected() = false
    override fun sendControllerState(state: ControllerState) {}
}
