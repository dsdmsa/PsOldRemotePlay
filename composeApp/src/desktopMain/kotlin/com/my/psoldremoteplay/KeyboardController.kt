package com.my.psoldremoteplay

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import com.my.psoldremoteplay.protocol.ControllerButtons
import com.my.psoldremoteplay.protocol.ControllerState

/**
 * Desktop keyboard input mapper.
 * Maps keyboard keys to PS3 controller buttons.
 *
 * Layout:
 * ```
 * Arrow Keys:  D-Pad (Up/Down/Left/Right)
 * WASD:        Left Analog Stick (W=up, S=down, A=left, D=right)
 * IJKL:        Right Analog Stick (I=up, K=down, J=left, L=right)
 * Q/E:         L1/R1 triggers
 * Z/X/C/V:     Square/Circle/Triangle/Cross buttons
 * Space:       Start
 * Shift:       Select
 * Tab:         PS (home button, currently unused)
 * ```
 */
object KeyboardController {
    private var currentState = ControllerState()
    private val pressedKeys = mutableSetOf<Key>()

    fun handleKeyDown(event: KeyEvent): ControllerState? {
        pressedKeys.add(event.key)
        return updateState()
    }

    fun handleKeyUp(event: KeyEvent): ControllerState? {
        pressedKeys.remove(event.key)
        return updateState()
    }

    private fun updateState(): ControllerState? {
        var newState = ControllerState()

        // D-Pad (arrow keys)
        if (pressedKeys.contains(Key.DirectionUp)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.UP)
        if (pressedKeys.contains(Key.DirectionDown)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.DOWN)
        if (pressedKeys.contains(Key.DirectionLeft)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.LEFT)
        if (pressedKeys.contains(Key.DirectionRight)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.RIGHT)

        // Left analog stick (WASD)
        var leftX = 0f
        var leftY = 0f
        if (pressedKeys.contains(Key.W)) leftY += 1f
        if (pressedKeys.contains(Key.S)) leftY -= 1f
        if (pressedKeys.contains(Key.A)) leftX -= 1f
        if (pressedKeys.contains(Key.D)) leftX += 1f
        newState = newState.copy(leftStickX = leftX.coerceIn(-1f, 1f), leftStickY = leftY.coerceIn(-1f, 1f))

        // Right analog stick (IJKL)
        var rightX = 0f
        var rightY = 0f
        if (pressedKeys.contains(Key.I)) rightY += 1f
        if (pressedKeys.contains(Key.K)) rightY -= 1f
        if (pressedKeys.contains(Key.J)) rightX -= 1f
        if (pressedKeys.contains(Key.L)) rightX += 1f
        newState = newState.copy(rightStickX = rightX.coerceIn(-1f, 1f), rightStickY = rightY.coerceIn(-1f, 1f))

        // Shoulder buttons (Q/E for L1/R1, Z/X for L2/R2)
        if (pressedKeys.contains(Key.Q)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.L1)
        if (pressedKeys.contains(Key.E)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.R1)
        if (pressedKeys.contains(Key.Z)) newState = newState.copy(l2 = 1f)
        if (pressedKeys.contains(Key.X)) newState = newState.copy(r2 = 1f)

        // Face buttons (C/V/T/Y for Square/Circle/Triangle/Cross)
        if (pressedKeys.contains(Key.C)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.SQUARE)
        if (pressedKeys.contains(Key.V)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.CROSS)
        if (pressedKeys.contains(Key.T)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.TRIANGLE)
        if (pressedKeys.contains(Key.Y)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.CIRCLE)

        // Start/Select
        if (pressedKeys.contains(Key.Enter)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.START)
        if (pressedKeys.contains(Key.Backspace)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.SELECT)

        // Stick clicks
        if (pressedKeys.contains(Key.NumPad1) || pressedKeys.contains(Key.One)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.L3)
        if (pressedKeys.contains(Key.NumPad3) || pressedKeys.contains(Key.Three)) newState = newState.copy(buttons = newState.buttons or ControllerButtons.R3)

        return if (newState != currentState) {
            currentState = newState
            newState
        } else {
            null  // No change
        }
    }

    fun reset() {
        pressedKeys.clear()
        currentState = ControllerState()
    }
}
