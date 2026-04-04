package com.my.psremoteplay.feature.ps3.platform

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.input.ControllerInputSender
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.feature.ps3.protocol.PadProtocol
import com.my.psremoteplay.feature.ps3.protocol.PremoConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

/**
 * Android controller input sender.
 * PLACEHOLDER: Logs intended packets.
 */
class AndroidControllerInput(private val logger: Logger) : ControllerInputSender {
    private var socket: Socket? = null
    private var isConnected = false

    override suspend fun connect(params: Map<String, String>) = withContext(Dispatchers.IO) {
        try {
            val ip = params["ip"] ?: error("Missing 'ip' in params")
            socket = Socket(ip, PremoConstants.PORT)
            isConnected = true
            logger.log("PAD", "[ANDROID] Controller connected")
        } catch (e: Exception) {
            logger.error("PAD", "[ANDROID] Connection failed", e)
            isConnected = false
        }
    }

    override suspend fun sendState(state: ControllerState) = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext
        try {
            PadProtocol.buildPadPacket(state)
            // TODO: Queue to batch, send when full
        } catch (e: Exception) {
            logger.error("PAD", "[ANDROID] Send error", e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
            isConnected = false
            logger.log("PAD", "[ANDROID] Controller disconnected")
        } catch (e: Exception) {
            logger.error("PAD", "[ANDROID] Disconnect error", e)
        }
    }
}
