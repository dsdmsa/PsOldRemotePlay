package com.my.psoldremoteplay

import com.my.psoldremoteplay.protocol.ControllerInputSender
import com.my.psoldremoteplay.protocol.ControllerState
import com.my.psoldremoteplay.protocol.PadProtocol
import com.my.psoldremoteplay.protocol.PremoConstants
import com.my.psoldremoteplay.protocol.PremoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

/**
 * Android controller input sender.
 * PLACEHOLDER: Logs intended packets.
 */
class AndroidControllerInput(private val logger: PremoLogger) : ControllerInputSender {
    private var socket: Socket? = null
    private var isConnected = false

    override suspend fun connect(ps3Ip: String, sessionId: String, authToken: String) = withContext(Dispatchers.IO) {
        try {
            socket = Socket(ps3Ip, PremoConstants.PORT)
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
