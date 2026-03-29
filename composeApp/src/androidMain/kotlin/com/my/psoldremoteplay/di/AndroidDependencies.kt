package com.my.psoldremoteplay.di

import com.my.psoldremoteplay.*
import com.my.psoldremoteplay.protocol.*
import com.my.psoldremoteplay.protocol.codec.AudioDecoder
import com.my.psoldremoteplay.protocol.codec.VideoDecoder
import com.my.psoldremoteplay.protocol.upscale.PassthroughUpscaler
import com.my.psoldremoteplay.protocol.upscale.UpscaleFilter

class AndroidDependencies : PlatformDependencies {
    override val logger: PremoLogger = object : PremoLogger {
        override fun log(tag: String, message: String) {
            android.util.Log.d(tag, message)
        }
        override fun error(tag: String, message: String, throwable: Throwable?) {
            android.util.Log.e(tag, message, throwable)
        }
    }

    override val crypto: PremoCrypto = AndroidPremoCrypto
    override val discoverer: Ps3Discoverer = AndroidPs3Discoverer(logger)
    override val sessionHandler: PremoSessionHandler = AndroidPremoSession(crypto, logger)
    override val registration: PremoRegistration = StubRegistration()
    override val videoDecoder: VideoDecoder = StubVideoDecoder()
    override val audioDecoder: AudioDecoder = StubAudioDecoder()
    override val videoRenderer: VideoRenderer = AndroidVideoRenderer(logger)
    override val audioRenderer: AudioRenderer = StubAudioRenderer(logger)
    override val upscaleFilter: UpscaleFilter = PassthroughUpscaler()
    override val controllerInput: ControllerInputSender = AndroidControllerInput(logger)
}
