package com.my.psremoteplay.feature.ps3.di

import com.my.psremoteplay.core.streaming.StreamingDependencies
import com.my.psremoteplay.core.streaming.input.ControllerInputSender
import com.my.psremoteplay.feature.ps3.protocol.Ps3Discoverer
import com.my.psremoteplay.feature.ps3.protocol.PremoSessionHandler
import com.my.psremoteplay.feature.ps3.protocol.PremoRegistration

interface Ps3Dependencies {
    val streaming: StreamingDependencies
    val discoverer: Ps3Discoverer
    val sessionHandler: PremoSessionHandler
    val registration: PremoRegistration
    val controllerInput: ControllerInputSender
}
