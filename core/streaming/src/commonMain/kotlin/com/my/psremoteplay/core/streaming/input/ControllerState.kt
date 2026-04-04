package com.my.psremoteplay.core.streaming.input

data class ControllerState(
    val buttons: Int = 0,
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val l2: Float = 0f,
    val r2: Float = 0f
)

object ControllerButtons {
    const val CROSS = 0x0001
    const val CIRCLE = 0x0002
    const val SQUARE = 0x0004
    const val TRIANGLE = 0x0008
    const val L1 = 0x0010
    const val R1 = 0x0020
    const val L2 = 0x0040
    const val R2 = 0x0080
    const val SELECT = 0x0100
    const val START = 0x0200
    const val L3 = 0x0400
    const val R3 = 0x0800
    const val UP = 0x1000
    const val DOWN = 0x2000
    const val LEFT = 0x4000
    const val RIGHT = 0x8000
    const val PS = 0x10000
}
