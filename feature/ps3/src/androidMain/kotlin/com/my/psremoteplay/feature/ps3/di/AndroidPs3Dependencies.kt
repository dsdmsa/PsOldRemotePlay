package com.my.psremoteplay.feature.ps3.di

import com.my.psremoteplay.core.streaming.Crypto
import com.my.psremoteplay.core.streaming.JvmCrypto
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.StreamingDependencies
import com.my.psremoteplay.core.streaming.audio.AudioRenderer
import com.my.psremoteplay.core.streaming.codec.AudioDecoder
import com.my.psremoteplay.core.streaming.codec.VideoDecoder
import com.my.psremoteplay.core.streaming.input.ControllerInputSender
import com.my.psremoteplay.core.streaming.stubs.LoggingVideoRenderer
import com.my.psremoteplay.core.streaming.stubs.StubAudioDecoder
import com.my.psremoteplay.core.streaming.stubs.StubAudioRenderer
import com.my.psremoteplay.core.streaming.stubs.StubVideoDecoder
import com.my.psremoteplay.core.streaming.upscale.PassthroughUpscaler
import com.my.psremoteplay.core.streaming.upscale.UpscaleFilter
import com.my.psremoteplay.core.streaming.video.VideoRenderer
import com.my.psremoteplay.feature.ps3.platform.AndroidControllerInput
import com.my.psremoteplay.feature.ps3.platform.AndroidPremoSession
import com.my.psremoteplay.feature.ps3.platform.AndroidPs3Discoverer
import com.my.psremoteplay.feature.ps3.platform.StubRegistration
import com.my.psremoteplay.feature.ps3.protocol.PremoRegistration
import com.my.psremoteplay.feature.ps3.protocol.PremoSessionHandler
import com.my.psremoteplay.feature.ps3.protocol.Ps3Discoverer

class AndroidPs3Dependencies : Ps3Dependencies {

    private val logger: Logger = object : Logger {
        override fun log(tag: String, message: String) {
            android.util.Log.d(tag, message)
        }
        override fun error(tag: String, message: String, throwable: Throwable?) {
            android.util.Log.e(tag, message, throwable)
        }
    }

    private val crypto: Crypto = JvmCrypto

    override val streaming: StreamingDependencies = object : StreamingDependencies {
        override val crypto: Crypto = this@AndroidPs3Dependencies.crypto
        override val videoDecoder: VideoDecoder = StubVideoDecoder()
        override val audioDecoder: AudioDecoder = StubAudioDecoder()
        override val videoRenderer: VideoRenderer = LoggingVideoRenderer(this@AndroidPs3Dependencies.logger)
        override val audioRenderer: AudioRenderer = StubAudioRenderer(this@AndroidPs3Dependencies.logger)
        override val upscaleFilter: UpscaleFilter = PassthroughUpscaler()
        override val logger: Logger = this@AndroidPs3Dependencies.logger
    }

    override val discoverer: Ps3Discoverer = AndroidPs3Discoverer(logger)
    override val sessionHandler: PremoSessionHandler = AndroidPremoSession(crypto, logger)
    override val registration: PremoRegistration = StubRegistration()
    override val controllerInput: ControllerInputSender = AndroidControllerInput(logger)
}
