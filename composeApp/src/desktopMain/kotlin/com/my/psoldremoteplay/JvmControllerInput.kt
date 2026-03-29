package com.my.psoldremoteplay

import com.my.psoldremoteplay.protocol.ControllerInputSender
import com.my.psoldremoteplay.protocol.ControllerState
import com.my.psoldremoteplay.protocol.PadProtocol
import com.my.psoldremoteplay.protocol.PremoConstants
import com.my.psoldremoteplay.protocol.PremoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.Socket

/**
 * Desktop controller input sender.
 * Sends PS3 controller state via POST /sce/premo/session/pad
 *
 * PLACEHOLDER: Logs intended packets. Real implementation queues to PS3.
 */
class JvmControllerInput(private val logger: PremoLogger) : ControllerInputSender {
    private var socket: Socket? = null
    private var isConnected = false

    override suspend fun connect(ps3Ip: String, sessionId: String, authToken: String) = withContext(Dispatchers.IO) {
        try {
            socket = Socket(ps3Ip, PremoConstants.PORT)
            isConnected = true
            logger.log("PAD", "[DESKTOP] Controller connected to $ps3Ip")
        } catch (e: Exception) {
            logger.error("PAD", "[DESKTOP] Controller connection failed", e)
            isConnected = false
        }
    }

    override suspend fun sendState(state: ControllerState) = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext
        try {
            val packet = PadProtocol.buildPadPacket(state)
            logger.log("PAD", "[DESKTOP] Pad state: buttons=${"%05X".format(state.buttons)}, sticks=(${state.leftStickX.toInt()},${state.leftStickY.toInt()})")
            // TODO: Queue packet to 60-packet batch, send to PS3 when batch full
        } catch (e: Exception) {
            logger.error("PAD", "[DESKTOP] Send error", e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
            isConnected = false
            logger.log("PAD", "[DESKTOP] Controller disconnected")
        } catch (e: Exception) {
            logger.error("PAD", "[DESKTOP] Disconnect error", e)
        }
    }
}
