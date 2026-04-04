package com.my.psremoteplay.feature.ps3.di

import com.my.psremoteplay.core.streaming.*
import com.my.psremoteplay.core.streaming.audio.AudioRenderer
import com.my.psremoteplay.core.streaming.codec.AudioDecoder
import com.my.psremoteplay.core.streaming.codec.VideoDecoder
import com.my.psremoteplay.core.streaming.input.ControllerInputSender
import com.my.psremoteplay.core.streaming.stubs.StubAudioDecoder
import com.my.psremoteplay.core.streaming.stubs.StubAudioRenderer
import com.my.psremoteplay.core.streaming.stubs.StubVideoDecoder
import com.my.psremoteplay.core.streaming.upscale.PassthroughUpscaler
import com.my.psremoteplay.core.streaming.upscale.UpscaleFilter
import com.my.psremoteplay.core.streaming.video.VideoRenderer
import com.my.psremoteplay.feature.ps3.platform.JvmControllerInput
import com.my.psremoteplay.feature.ps3.platform.JvmPremoRegistration
import com.my.psremoteplay.feature.ps3.platform.JvmPremoSession
import com.my.psremoteplay.feature.ps3.platform.JvmPs3Discoverer
import com.my.psremoteplay.feature.ps3.protocol.PremoRegistration
import com.my.psremoteplay.feature.ps3.protocol.PremoSessionHandler
import com.my.psremoteplay.feature.ps3.protocol.Ps3Discoverer

class DesktopPs3Dependencies : Ps3Dependencies {

    private val logger: Logger = object : Logger {
        override fun log(tag: String, message: String) {
            println("[$tag] $message")
        }
        override fun error(tag: String, message: String, throwable: Throwable?) {
            System.err.println("[ERROR:$tag] $message")
            throwable?.printStackTrace(System.err)
        }
    }

    private val crypto: Crypto = JvmCrypto

    override val streaming: StreamingDependencies = object : StreamingDependencies {
        override val crypto: Crypto = this@DesktopPs3Dependencies.crypto
        override val videoDecoder: VideoDecoder = StubVideoDecoder()
        override val audioDecoder: AudioDecoder = StubAudioDecoder()
        override val videoRenderer: VideoRenderer = JvmVideoRenderer(this@DesktopPs3Dependencies.logger)
        override val audioRenderer: AudioRenderer = StubAudioRenderer(this@DesktopPs3Dependencies.logger)
        override val upscaleFilter: UpscaleFilter = PassthroughUpscaler()
        override val logger: Logger = this@DesktopPs3Dependencies.logger
    }

    override val discoverer: Ps3Discoverer = JvmPs3Discoverer(logger)
    override val sessionHandler: PremoSessionHandler = JvmPremoSession(crypto, logger)
    override val registration: PremoRegistration = JvmPremoRegistration(crypto, logger)
    override val controllerInput: ControllerInputSender = JvmControllerInput(logger)
}
