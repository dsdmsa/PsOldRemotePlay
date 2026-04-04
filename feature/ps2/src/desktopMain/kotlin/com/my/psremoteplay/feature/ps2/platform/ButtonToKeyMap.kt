package com.my.psremoteplay.feature.ps2.platform

import com.my.psremoteplay.core.streaming.input.ControllerButtons
import java.awt.event.KeyEvent

object ButtonToKeyMap {
    val mapping: Map<Int, Int> = mapOf(
        ControllerButtons.CROSS    to KeyEvent.VK_ENTER,
        ControllerButtons.CIRCLE   to KeyEvent.VK_BACK_SPACE,
        ControllerButtons.SQUARE   to KeyEvent.VK_SHIFT,
        ControllerButtons.TRIANGLE to KeyEvent.VK_SPACE,
        ControllerButtons.UP       to KeyEvent.VK_UP,
        ControllerButtons.DOWN     to KeyEvent.VK_DOWN,
        ControllerButtons.LEFT     to KeyEvent.VK_LEFT,
        ControllerButtons.RIGHT    to KeyEvent.VK_RIGHT,
        ControllerButtons.START    to KeyEvent.VK_F1,
        ControllerButtons.SELECT   to KeyEvent.VK_F2,
        ControllerButtons.L1       to KeyEvent.VK_Q,
        ControllerButtons.R1       to KeyEvent.VK_E,
        ControllerButtons.L2       to KeyEvent.VK_1,
        ControllerButtons.R2       to KeyEvent.VK_2,
        ControllerButtons.L3       to KeyEvent.VK_3,
        ControllerButtons.R3       to KeyEvent.VK_4,
    )

    // Analog stick direction keys (digital threshold)
    val leftStickUp = KeyEvent.VK_W
    val leftStickDown = KeyEvent.VK_S
    val leftStickLeft = KeyEvent.VK_A
    val leftStickRight = KeyEvent.VK_D
    val rightStickUp = KeyEvent.VK_I
    val rightStickDown = KeyEvent.VK_K
    val rightStickLeft = KeyEvent.VK_J
    val rightStickRight = KeyEvent.VK_L
}
