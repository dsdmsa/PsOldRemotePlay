package com.my.psremoteplay.app.ps2client

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.my.psremoteplay.core.streaming.input.ControllerButtons
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.feature.ps2.di.AndroidPs2ClientDependencies
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientEffect
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientIntent
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientViewModel
import com.my.psremoteplay.feature.ps2.strategy.StreamingPreset
import com.my.psremoteplay.feature.ps2.ui.Ps2AndroidGameScreen
import com.my.psremoteplay.feature.ps2.ui.Ps2AndroidHwGameScreen
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private var viewModel: Ps2ClientViewModel? = null
    private var controllerState = ControllerState()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val deps = remember { AndroidPs2ClientDependencies(StreamingPreset.H264_HW) }
            val vm = viewModel { Ps2ClientViewModel(deps) }
            viewModel = vm

            LaunchedEffect(Unit) {
                vm.effects.collectLatest { effect ->
                    when (effect) {
                        is Ps2ClientEffect.ShowMessage -> {
                            android.widget.Toast.makeText(this@MainActivity, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                vm.onIntent(Ps2ClientIntent.UpdateServerIp("192.168.1.128"))
                vm.onIntent(Ps2ClientIntent.Connect)
            }

            val state by vm.state.collectAsState()

            // Always use HW screen for H264_HW preset — shows retry button on error,
            // FSR upscaling during streaming, and controller overlay when connected
            Ps2AndroidHwGameScreen(
                onSurfaceAvailable = { surface -> deps.setSurface(surface) },
                onSurfaceDestroyed = { deps.setSurface(null) },
                onControllerState = { vm.onIntent(Ps2ClientIntent.ControllerInput(it)) },
                onClose = { finish() },
                onReconnect = { vm.onIntent(Ps2ClientIntent.Reconnect) },
                statusText = state.statusText,
                frameCount = state.videoFrameCount,
                isError = state.connectionStatus == com.my.psremoteplay.core.streaming.ConnectionStatus.Error
            )
        }
    }

    // PS5 DualSense button mapping → PS2 ControllerButtons
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.source?.and(InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            val button = mapKeyToButton(keyCode)
            if (button != 0) {
                controllerState = controllerState.copy(buttons = controllerState.buttons or button)
                viewModel?.onIntent(Ps2ClientIntent.ControllerInput(controllerState))
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.source?.and(InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            val button = mapKeyToButton(keyCode)
            if (button != 0) {
                controllerState = controllerState.copy(buttons = controllerState.buttons and button.inv())
                viewModel?.onIntent(Ps2ClientIntent.ControllerInput(controllerState))
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onGenericMotionEvent(event)
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE) {

            // Left stick
            val lx = event.getAxisValue(MotionEvent.AXIS_X)
            val ly = -event.getAxisValue(MotionEvent.AXIS_Y) // invert Y (up = positive)

            // Right stick
            val rx = event.getAxisValue(MotionEvent.AXIS_Z)
            val ry = -event.getAxisValue(MotionEvent.AXIS_RZ)

            // Triggers (L2/R2 analog)
            val l2 = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
            val r2 = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)

            // D-pad from hat axes
            val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            var dpad = controllerState.buttons and (ControllerButtons.UP or ControllerButtons.DOWN or ControllerButtons.LEFT or ControllerButtons.RIGHT).inv()
            if (hatX < -0.5f) dpad = dpad or ControllerButtons.LEFT
            if (hatX > 0.5f) dpad = dpad or ControllerButtons.RIGHT
            if (hatY < -0.5f) dpad = dpad or ControllerButtons.UP
            if (hatY > 0.5f) dpad = dpad or ControllerButtons.DOWN

            controllerState = controllerState.copy(
                leftStickX = lx, leftStickY = ly,
                rightStickX = rx, rightStickY = ry,
                l2 = l2, r2 = r2,
                buttons = (controllerState.buttons and (ControllerButtons.UP or ControllerButtons.DOWN or ControllerButtons.LEFT or ControllerButtons.RIGHT).inv()) or
                    (dpad and (ControllerButtons.UP or ControllerButtons.DOWN or ControllerButtons.LEFT or ControllerButtons.RIGHT))
            )
            viewModel?.onIntent(Ps2ClientIntent.ControllerInput(controllerState))
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    /**
     * Map PS5 DualSense key codes to PS2 controller buttons.
     * DualSense on Android uses standard HID gamepad mapping.
     */
    private fun mapKeyToButton(keyCode: Int): Int = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> ControllerButtons.CROSS      // X button (bottom)
        KeyEvent.KEYCODE_BUTTON_B -> ControllerButtons.CIRCLE     // O button (right)
        KeyEvent.KEYCODE_BUTTON_X -> ControllerButtons.SQUARE     // Square (left)
        KeyEvent.KEYCODE_BUTTON_Y -> ControllerButtons.TRIANGLE   // Triangle (top)
        KeyEvent.KEYCODE_BUTTON_L1 -> ControllerButtons.L1
        KeyEvent.KEYCODE_BUTTON_R1 -> ControllerButtons.R1
        KeyEvent.KEYCODE_BUTTON_L2 -> ControllerButtons.L2
        KeyEvent.KEYCODE_BUTTON_R2 -> ControllerButtons.R2
        KeyEvent.KEYCODE_BUTTON_THUMBL -> ControllerButtons.L3    // Left stick press
        KeyEvent.KEYCODE_BUTTON_THUMBR -> ControllerButtons.R3    // Right stick press
        KeyEvent.KEYCODE_BUTTON_SELECT -> ControllerButtons.SELECT // Share/Create
        KeyEvent.KEYCODE_BUTTON_START -> ControllerButtons.START   // Options
        KeyEvent.KEYCODE_BUTTON_MODE -> ControllerButtons.PS       // PS button
        KeyEvent.KEYCODE_DPAD_UP -> ControllerButtons.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> ControllerButtons.DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> ControllerButtons.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> ControllerButtons.RIGHT
        else -> 0
    }
}
