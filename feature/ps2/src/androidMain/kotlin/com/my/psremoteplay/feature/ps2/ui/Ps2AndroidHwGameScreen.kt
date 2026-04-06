package com.my.psremoteplay.feature.ps2.ui

import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psremoteplay.core.streaming.input.ControllerButtons
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.feature.ps2.ui.upscale.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Ps2AndroidHwGameScreen(
    onSurfaceAvailable: (Surface) -> Unit,
    onSurfaceDestroyed: () -> Unit,
    onControllerState: (ControllerState) -> Unit,
    onClose: () -> Unit,
    onReconnect: () -> Unit = {},
    statusText: String = "",
    frameCount: Int = 0,
    isError: Boolean = false,
    upscaleMethod: UpscaleMethod = UpscaleMethod.CATMULL_ROM,
    sharpenMethod: SharpenMethod = SharpenMethod.CAS,
    sharpness: Float = 0.2f,
    bitrateMbps: Int = 8,
    onUpscaleMethodChanged: (UpscaleMethod) -> Unit = {},
    onSharpenMethodChanged: (SharpenMethod) -> Unit = {},
    onSharpnessChanged: (Float) -> Unit = {},
    onBitrateChanged: (Int) -> Unit = {},
    decodeFps: Int = 0,
    pingMs: Int = -1,
    modifier: Modifier = Modifier
) {
    var controllerState by remember { mutableStateOf(ControllerState()) }
    val scope = rememberCoroutineScope()
    var renderer by remember { mutableStateOf<FsrRenderer?>(null) }

    fun send(newState: ControllerState) {
        controllerState = newState
        onControllerState(newState)
    }

    fun tapButton(button: Int) {
        send(controllerState.copy(buttons = controllerState.buttons or button))
        scope.launch {
            delay(100)
            send(controllerState.copy(buttons = controllerState.buttons and button.inv()))
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Video layer
        GameStreamSurface(
            modifier = Modifier.fillMaxSize(),
            upscaleMethod = upscaleMethod,
            sharpenMethod = sharpenMethod,
            sharpness = sharpness,
            onSurfaceAvailable = onSurfaceAvailable,
            onSurfaceDestroyed = onSurfaceDestroyed,
            onRendererReady = { renderer = it }
        )

        // Status overlay (error or waiting)
        if (frameCount == 0 || isError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        statusText.ifEmpty { "Waiting for H.264 stream..." },
                        color = if (isError) Color.Red else Color.Cyan,
                        fontSize = 16.sp, fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onReconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan.copy(alpha = 0.3f))
                    ) { Text("Retry Connection", color = Color.White, fontFamily = FontFamily.Monospace) }
                }
            }
        }

        // Touch zones (only during streaming)
        if (frameCount > 0 && !isError)
        Row(Modifier.fillMaxSize()) {
            HwStickTouchZone(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onTap = { dx, dy ->
                    val btn = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                        if (dx > 0) ControllerButtons.RIGHT else ControllerButtons.LEFT
                    } else { if (dy > 0) ControllerButtons.DOWN else ControllerButtons.UP }
                    tapButton(btn)
                },
                onStickMove = { x, y -> send(controllerState.copy(leftStickX = x, leftStickY = y)) },
                onStickRelease = { send(controllerState.copy(leftStickX = 0f, leftStickY = 0f)) }
            )
            HwStickTouchZone(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onTap = { dx, dy ->
                    val btn = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                        if (dx > 0) ControllerButtons.CIRCLE else ControllerButtons.SQUARE
                    } else { if (dy > 0) ControllerButtons.CROSS else ControllerButtons.TRIANGLE }
                    tapButton(btn)
                },
                onStickMove = { x, y -> send(controllerState.copy(rightStickX = x, rightStickY = y)) },
                onStickRelease = { send(controllerState.copy(rightStickX = 0f, rightStickY = 0f)) }
            )
        }

        // Bottom bar (only during streaming)
        if (frameCount > 0 && !isError)
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HwTouchBtn("L1") { tapButton(ControllerButtons.L1) }
                HwTouchBtn("L2") { tapButton(ControllerButtons.L2) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HwTouchBtn("SEL") { tapButton(ControllerButtons.SELECT) }
                HwTouchBtn("STR") { tapButton(ControllerButtons.START) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HwTouchBtn("R1") { tapButton(ControllerButtons.R1) }
                HwTouchBtn("R2") { tapButton(ControllerButtons.R2) }
            }
        }

        // Top-right: reconnect + close
        Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onReconnect, modifier = Modifier.size(36.dp), shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(0.dp)
            ) { Text("↻", color = Color.White, fontSize = 16.sp) }
            Button(onClick = onClose, modifier = Modifier.size(36.dp), shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(0.dp)
            ) { Text("X", color = Color.White, fontSize = 14.sp) }
        }

        // Performance overlay: FPS + ping
        if (frameCount > 0) {
            val renderFps = renderer?.currentFps ?: 0
            val fpsColor = when {
                decodeFps >= 25 -> Color.Green
                decodeFps >= 15 -> Color.Yellow
                else -> Color.Red
            }
            Column(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Text(
                    "${decodeFps}fps decode | ${renderFps}fps render",
                    color = fpsColor.copy(alpha = 0.8f),
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace
                )
                if (pingMs >= 0) {
                    val pingColor = when {
                        pingMs < 20 -> Color.Green
                        pingMs < 50 -> Color.Yellow
                        else -> Color.Red
                    }
                    Text(
                        "${pingMs}ms ping",
                        color = pingColor.copy(alpha = 0.8f),
                        fontSize = 10.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Settings drawer
        UpscaleSettingsDrawer(
            upscaleMethod = upscaleMethod,
            sharpenMethod = sharpenMethod,
            sharpness = sharpness,
            bitrateMbps = bitrateMbps,
            onUpscaleMethodChanged = onUpscaleMethodChanged,
            onSharpenMethodChanged = onSharpenMethodChanged,
            onSharpnessChanged = onSharpnessChanged,
            onBitrateChanged = onBitrateChanged
        )
    }
}

@Composable
private fun HwStickTouchZone(modifier: Modifier, onTap: (Float, Float) -> Unit, onStickMove: (Float, Float) -> Unit, onStickRelease: () -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var stickX by remember { mutableStateOf(0f) }
    var stickY by remember { mutableStateOf(0f) }
    Box(modifier.onSizeChanged { size = it }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = { stickX = 0f; stickY = 0f; onStickRelease() },
                onDragCancel = { stickX = 0f; stickY = 0f; onStickRelease() }
            ) { _, d -> stickX = (stickX + d.x/100f).coerceIn(-1f,1f); stickY = (stickY - d.y/100f).coerceIn(-1f,1f); onStickMove(stickX, stickY) }
        }
        .pointerInput(Unit) {
            detectTapGestures { o ->
                if (size.width > 0 && size.height > 0) onTap(o.x - size.width/2f, o.y - size.height/2f)
            }
        }
    )
}

@Composable
private fun HwTouchBtn(label: String, onClick: () -> Unit) {
    Button(onClick, Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
    ) { Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
}
