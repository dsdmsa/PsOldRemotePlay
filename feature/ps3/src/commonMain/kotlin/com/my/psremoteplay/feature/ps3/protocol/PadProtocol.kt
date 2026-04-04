package com.my.psremoteplay.feature.ps3.protocol

import com.my.psremoteplay.core.streaming.input.ControllerButtons
import com.my.psremoteplay.core.streaming.input.ControllerState
import kotlin.math.roundToInt

object PadProtocol {
    fun buildPadPacket(state: ControllerState): ByteArray {
        val packet = ByteArray(128)
        packet[0x00] = 0x74
        packet[0x04] = (0x80 + (state.leftStickX * 127)).roundToInt().toByte()
        packet[0x05] = (0x80 + (state.leftStickY * 127)).roundToInt().toByte()
        packet[0x08] = (0x80 + (state.rightStickX * 127)).roundToInt().toByte()
        packet[0x09] = (0x80 + (state.rightStickY * 127)).roundToInt().toByte()

        packet[0x05] = buildButtonByte(state.buttons, listOf(
            ControllerButtons.SQUARE, ControllerButtons.CROSS,
            ControllerButtons.CIRCLE, ControllerButtons.TRIANGLE,
            ControllerButtons.R1, ControllerButtons.L1,
            ControllerButtons.R2, ControllerButtons.L2
        )).toByte()

        packet[0x07] = buildButtonByte(state.buttons, listOf(
            ControllerButtons.LEFT, ControllerButtons.DOWN,
            ControllerButtons.RIGHT, ControllerButtons.UP,
            ControllerButtons.START, ControllerButtons.R3,
            ControllerButtons.L3, ControllerButtons.SELECT
        )).toByte()

        packet[0x06] = (state.l2 * 255).roundToInt().toByte()
        packet[0x0A] = (state.r2 * 255).roundToInt().toByte()

        return packet
    }

    private fun buildButtonByte(buttons: Int, buttonMasks: List<Int>): Int {
        var byte = 0
        for (i in buttonMasks.indices) {
            if ((buttons and buttonMasks[i]) != 0) {
                byte = byte or (1 shl i)
            }
        }
        return byte
    }
}
