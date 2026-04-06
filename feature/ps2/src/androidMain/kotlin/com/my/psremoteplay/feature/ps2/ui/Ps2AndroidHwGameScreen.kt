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

/**
 * Hardware-accelerated game screen.
 *
 * Video: SurfaceView → MediaCodec decodes directly to Surface (zero-copy, no Compose).
 * Controls: Compose overlay on top of the SurfaceView hardware layer.
 *
 * This eliminates the ImageBitmap → Compose recomposition → Skia draw → GPU upload
 * bottleneck, yielding ~15-20 FPS improvement over the software path.
 */
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
    currentUpscalePreset: UpscalePreset = UpscalePreset.NONE,
    upscaleSharpness: Float = 0.5f,
    onUpscalePresetChanged: (UpscalePreset) -> Unit = {},
    onUpscaleSharpnessChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var controllerState by remember { mutableStateOf(ControllerState()) }
    val scope = rememberCoroutineScope()

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

    // Map preset to strategy
    val upscaleStrategy = remember(currentUpscalePreset) {
        when (currentUpscalePreset) {
            UpscalePreset.NONE -> null
            UpscalePreset.FSR -> FsrStrategy()
            UpscalePreset.SGSR -> SgsrStrategy()
            UpscalePreset.CATMULL_ROM_CAS -> CatmullRomCasStrategy()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Video layer with upscaling
        GameStreamSurface(
            modifier = Modifier.fillMaxSize(),
            upscaleStrategy = upscaleStrategy,
            sharpness = upscaleSharpness,
            onSurfaceAvailable = onSurfaceAvailable,
            onSurfaceDestroyed = onSurfaceDestroyed
        )

        // Status overlay (before stream starts or on error)
        if (frameCount == 0 || isError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        statusText.ifEmpty { "Waiting for H.264 stream..." },
                        color = if (isError) Color.Red else Color.Cyan,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onReconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan.copy(alpha = 0.3f))
                    ) {
                        Text("Retry Connection", color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Touch zones: only active during streaming (hidden on error so retry button is tappable)
        if (frameCount > 0 && !isError)
        Row(Modifier.fillMaxSize()) {
            HwStickTouchZone(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onTap = { dx, dy ->
                    val btn = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                        if (dx > 0) ControllerButtons.RIGHT else ControllerButtons.LEFT
                    } else {
                        if (dy > 0) ControllerButtons.DOWN else ControllerButtons.UP
                    }
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
                    } else {
                        if (dy > 0) ControllerButtons.CROSS else ControllerButtons.TRIANGLE
                    }
                    tapButton(btn)
                },
                onStickMove = { x, y -> send(controllerState.copy(rightStickX = x, rightStickY = y)) },
                onStickRelease = { send(controllerState.copy(rightStickX = 0f, rightStickY = 0f)) }
            )
        }

        // Bottom bar: shoulder buttons + Select/Start (only during streaming)
        if (frameCount > 0 && !isError)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
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

        // Top-right buttons: reconnect + close
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onReconnect,
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(0.dp)
            ) { Text("↻", color = Color.White, fontSize = 16.sp) }
            Button(
                onClick = onClose,
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(0.dp)
            ) { Text("X", color = Color.White, fontSize = 14.sp) }
        }

        // Performance overlay
        if (frameCount > 0) {
            Text(
                "HW $frameCount",
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                color = Color.Green.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Upscale settings drawer (renders on top of all other content)
        UpscaleSettingsDrawer(
            currentPreset = currentUpscalePreset,
            sharpness = upscaleSharpness,
            onPresetChanged = onUpscalePresetChanged,
            onSharpnessChanged = onUpscaleSharpnessChanged
        )
    }
}

@Composable
private fun HwStickTouchZone(
    modifier: Modifier,
    onTap: (dx: Float, dy: Float) -> Unit,
    onStickMove: (x: Float, y: Float) -> Unit,
    onStickRelease: () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var stickX by remember { mutableStateOf(0f) }
    var stickY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { stickX = 0f; stickY = 0f; onStickRelease() },
                    onDragCancel = { stickX = 0f; stickY = 0f; onStickRelease() }
                ) { _, dragAmount ->
                    stickX = (stickX + dragAmount.x / 100f).coerceIn(-1f, 1f)
                    stickY = (stickY - dragAmount.y / 100f).coerceIn(-1f, 1f)
                    onStickMove(stickX, stickY)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (size.width == 0 || size.height == 0) return@detectTapGestures
                    val dx = offset.x - size.width / 2f
                    val dy = offset.y - size.height / 2f
                    onTap(dx, dy)
                }
            }
    )
}

@Composable
private fun HwTouchBtn(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
    ) { Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
}
