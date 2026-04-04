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
 * Desktop controller input sender.
 * Sends PS3 controller state via POST /sce/premo/session/pad
 *
 * PLACEHOLDER: Logs intended packets. Real implementation queues to PS3.
 */
class JvmControllerInput(private val logger: Logger) : ControllerInputSender {
    private var socket: Socket? = null
    private var isConnected = false

    override suspend fun connect(params: Map<String, String>) = withContext(Dispatchers.IO) {
        try {
            val ps3Ip = params["ip"] ?: error("Missing 'ip' parameter")
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
