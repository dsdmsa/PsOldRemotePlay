package com.my.psoldremoteplay.protocol

import kotlin.math.roundToInt

/**
 * PS3 pad (controller) packet builder.
 * Builds 128-byte structs for sending controller state to PS3.
 *
 * Endpoint: POST /sce/premo/session/pad
 * Body: 60 × 128-byte packets (7680 bytes total) per cycle
 * Cycle: send 60 packets → read 80-byte ACK → repeat
 */
object PadProtocol {
    /**
     * Build a single 128-byte pad packet from ControllerState.
     */
    fun buildPadPacket(state: ControllerState): ByteArray {
        val packet = ByteArray(128)

        // Magic constant
        packet[0x00] = 0x74

        // Left analog stick (center = 0x80, range 0x00-0xFF)
        packet[0x04] = (0x80 + (state.leftStickX * 127)).roundToInt().toByte()
        packet[0x05] = (0x80 + (state.leftStickY * 127)).roundToInt().toByte()

        // Right analog stick
        packet[0x08] = (0x80 + (state.rightStickX * 127)).roundToInt().toByte()
        packet[0x09] = (0x80 + (state.rightStickY * 127)).roundToInt().toByte()

        // Buttons — two bytes: high byte + low byte
        // High byte [0x05]: L2/R2/L1/R1/Triangle/Circle/Cross/Square
        // Low byte  [0x07]: Select/L3/R3/Start/Up/Right/Down/Left
        packet[0x05] = buildButtonByte(state.buttons, listOf(
            ControllerButtons.SQUARE,
            ControllerButtons.CROSS,
            ControllerButtons.CIRCLE,
            ControllerButtons.TRIANGLE,
            ControllerButtons.R1,
            ControllerButtons.L1,
            ControllerButtons.R2,
            ControllerButtons.L2
        )).toByte()

        packet[0x07] = buildButtonByte(state.buttons, listOf(
            ControllerButtons.LEFT,
            ControllerButtons.DOWN,
            ControllerButtons.RIGHT,
            ControllerButtons.UP,
            ControllerButtons.START,
            ControllerButtons.R3,
            ControllerButtons.L3,
            ControllerButtons.SELECT
        )).toByte()

        // Analog trigger values (L2, R2 as bytes 0-255)
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
