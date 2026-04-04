package com.my.psremoteplay.feature.ps2.di

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.StreamingDependencies
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamClient

interface Ps2ClientDependencies {
    val streaming: StreamingDependencies
    val logger: Logger

    /** Active video stream client strategy — swap to change receive+decode pipeline */
    val videoStreamClient: VideoStreamClient

    /** Default stream configuration */
    val streamConfig: StreamConfig

    /** Control channel (TCP) for server info + controller input */
    fun connectControl(ip: String, port: Int, onServerInfo: (String) -> Unit): Boolean
    fun disconnectControl()
    fun isConnected(): Boolean
    fun sendControllerState(state: ControllerState)
}
