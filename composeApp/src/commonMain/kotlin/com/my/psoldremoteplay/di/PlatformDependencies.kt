package com.my.psoldremoteplay.di

import com.my.psoldremoteplay.protocol.*
import com.my.psoldremoteplay.protocol.codec.AudioDecoder
import com.my.psoldremoteplay.protocol.codec.VideoDecoder
import com.my.psoldremoteplay.protocol.upscale.UpscaleFilter

interface PlatformDependencies {
    val crypto: PremoCrypto
    val discoverer: Ps3Discoverer
    val sessionHandler: PremoSessionHandler
    val registration: PremoRegistration
    val videoDecoder: VideoDecoder
    val audioDecoder: AudioDecoder
    val videoRenderer: VideoRenderer
    val audioRenderer: AudioRenderer
    val upscaleFilter: UpscaleFilter
    val controllerInput: ControllerInputSender
    val logger: PremoLogger
}
