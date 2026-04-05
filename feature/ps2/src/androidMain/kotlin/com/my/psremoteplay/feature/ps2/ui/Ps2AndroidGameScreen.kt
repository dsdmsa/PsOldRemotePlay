package com.my.psremoteplay.feature.ps2.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psremoteplay.core.streaming.input.ControllerButtons
import com.my.psremoteplay.core.streaming.input.ControllerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen Android game screen with invisible touch zones.
 *
 * Left half: Drag = left stick, Tap = D-pad direction (auto-releases after 100ms)
 * Right half: Drag = right stick, Tap = face button (auto-releases after 100ms)
 * Bottom: L1/L2, Select/Start, R1/R2 buttons
 * Top-right: Close button
 */
@Composable
fun Ps2AndroidGameScreen(
    currentFrame: ImageBitmap?,
    onControllerState: (ControllerState) -> Unit,
    onClose: () -> Unit,
    statusText: String = "",
    frameCount: Int = 0,
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

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Video
        if (currentFrame != null) {
            Image(
                bitmap = currentFrame,
                contentDescription = "Game stream",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (frameCount > 0) "Buffering..." else statusText.ifEmpty { "Connecting..." },
                    color = Color.Cyan, fontSize = 16.sp, fontFamily = FontFamily.Monospace
                )
            }
        }

        // Touch zones (left + right halves)
        Row(Modifier.fillMaxSize()) {
            // LEFT: D-pad tap + left stick drag
            StickTouchZone(
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

            // RIGHT: Face button tap + right stick drag
            StickTouchZone(
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

        // Bottom: shoulder + Select/Start
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TouchBtn("L1") { tapButton(ControllerButtons.L1) }
                TouchBtn("L2") { tapButton(ControllerButtons.L2) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TouchBtn("SEL") { tapButton(ControllerButtons.SELECT) }
                TouchBtn("STR") { tapButton(ControllerButtons.START) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TouchBtn("R1") { tapButton(ControllerButtons.R1) }
                TouchBtn("R2") { tapButton(ControllerButtons.R2) }
            }
        }

        // Close button
        Button(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(36.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(0.dp)
        ) { Text("X", color = Color.White, fontSize = 14.sp) }
    }
}

@Composable
private fun StickTouchZone(
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
private fun TouchBtn(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
    ) { Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
}
