package com.my.psremoteplay.feature.ps2.platform

import com.my.psremoteplay.core.streaming.input.ControllerButtons
import java.awt.event.KeyEvent

object ButtonToKeyMap {
    /**
     * Maps PS2 controller buttons to keyboard keys matching PCSX2 v2.x defaults.
     * PCSX2 uses: Z=Cross, X=Circle, A=Square, S=Triangle, Enter=Start, Backspace=Select.
     */
    val mapping: Map<Int, Int> = mapOf(
        ControllerButtons.CROSS    to KeyEvent.VK_Z,
        ControllerButtons.CIRCLE   to KeyEvent.VK_X,
        ControllerButtons.SQUARE   to KeyEvent.VK_A,
        ControllerButtons.TRIANGLE to KeyEvent.VK_S,
        ControllerButtons.UP       to KeyEvent.VK_UP,
        ControllerButtons.DOWN     to KeyEvent.VK_DOWN,
        ControllerButtons.LEFT     to KeyEvent.VK_LEFT,
        ControllerButtons.RIGHT    to KeyEvent.VK_RIGHT,
        ControllerButtons.START    to KeyEvent.VK_ENTER,
        ControllerButtons.SELECT   to KeyEvent.VK_BACK_SPACE,
        ControllerButtons.L1       to KeyEvent.VK_Q,
        ControllerButtons.R1       to KeyEvent.VK_E,
        ControllerButtons.L2       to KeyEvent.VK_1,
        ControllerButtons.R2       to KeyEvent.VK_3,
        ControllerButtons.L3       to KeyEvent.VK_F,
        ControllerButtons.R3       to KeyEvent.VK_G,
    )

    // Analog stick direction keys (PCSX2 defaults: WASD for L-stick, TFGH for R-stick)
    val leftStickUp = KeyEvent.VK_W
    val leftStickDown = KeyEvent.VK_S
    val leftStickLeft = KeyEvent.VK_A
    val leftStickRight = KeyEvent.VK_D
    val rightStickUp = KeyEvent.VK_T
    val rightStickDown = KeyEvent.VK_G
    val rightStickLeft = KeyEvent.VK_F
    val rightStickRight = KeyEvent.VK_H
}
