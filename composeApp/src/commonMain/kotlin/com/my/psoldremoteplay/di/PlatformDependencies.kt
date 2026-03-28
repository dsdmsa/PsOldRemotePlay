package com.my.psoldremoteplay.di

import com.my.psoldremoteplay.protocol.*

interface PlatformDependencies {
    val crypto: PremoCrypto
    val discoverer: Ps3Discoverer
    val sessionHandler: PremoSessionHandler
    val registration: PremoRegistration
    val videoRenderer: VideoRenderer
    val controllerInput: ControllerInputSender
    val logger: PremoLogger
}
