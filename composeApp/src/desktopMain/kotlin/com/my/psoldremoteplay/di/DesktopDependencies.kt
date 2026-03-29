package com.my.psoldremoteplay.di

import com.my.psoldremoteplay.*
import com.my.psoldremoteplay.protocol.*
import com.my.psoldremoteplay.protocol.codec.AudioDecoder
import com.my.psoldremoteplay.protocol.codec.VideoDecoder
import com.my.psoldremoteplay.protocol.upscale.PassthroughUpscaler
import com.my.psoldremoteplay.protocol.upscale.UpscaleFilter

class DesktopDependencies : PlatformDependencies {
    override val logger: PremoLogger = object : PremoLogger {
        override fun log(tag: String, message: String) {
            println("[$tag] $message")
        }
        override fun error(tag: String, message: String, throwable: Throwable?) {
            System.err.println("[ERROR:$tag] $message")
            throwable?.printStackTrace(System.err)
        }
    }

    override val crypto: PremoCrypto = JvmPremoCrypto
    override val discoverer: Ps3Discoverer = JvmPs3Discoverer(logger)
    override val sessionHandler: PremoSessionHandler = JvmPremoSession(crypto, logger)
    override val registration: PremoRegistration = JvmPremoRegistration(crypto, logger)
    override val videoDecoder: VideoDecoder = StubVideoDecoder()
    override val audioDecoder: AudioDecoder = StubAudioDecoder()
    override val videoRenderer: VideoRenderer = JvmVideoRenderer(logger)
    override val audioRenderer: AudioRenderer = StubAudioRenderer(logger)
    override val upscaleFilter: UpscaleFilter = PassthroughUpscaler()
    override val controllerInput: ControllerInputSender = JvmControllerInput(logger)
}
