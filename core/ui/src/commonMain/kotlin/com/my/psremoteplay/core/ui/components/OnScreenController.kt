package com.my.psremoteplay.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psremoteplay.core.streaming.input.ControllerButtons
import com.my.psremoteplay.core.streaming.input.ControllerState

@Composable
fun OnScreenController(
    onStateChange: (ControllerState) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    if (!isVisible) return

    var currentState by remember { mutableStateOf(ControllerState()) }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.width(120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("D-Pad", color = Color.Cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                DPadController { buttons ->
                    currentState = currentState.copy(buttons = buttons)
                    onStateChange(currentState)
                }
            }

            Column(
                modifier = Modifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("L-Stick", color = Color.Cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                AnalogStickController(
                    onMove = { x, y ->
                        currentState = currentState.copy(leftStickX = x, leftStickY = y)
                        onStateChange(currentState)
                    }
                )
            }

            Column(
                modifier = Modifier.width(120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Buttons", color = Color.Cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                FaceButtonsController { newState ->
                    currentState = newState
                    onStateChange(currentState)
                }
            }
        }
    }
}

@Composable
private fun DPadController(onButtonsChange: (Int) -> Unit) {
    var buttons by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.size(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DPadButton(label = "^", onClick = {
            buttons = buttons xor ControllerButtons.UP
            onButtonsChange(buttons)
        })
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.height(24.dp)) {
            DPadButton(label = "<", onClick = {
                buttons = buttons xor ControllerButtons.LEFT
                onButtonsChange(buttons)
            })
            Spacer(Modifier.width(8.dp))
            DPadButton(label = "o", onClick = {})
            Spacer(Modifier.width(8.dp))
            DPadButton(label = ">", onClick = {
                buttons = buttons xor ControllerButtons.RIGHT
                onButtonsChange(buttons)
            })
        }
        DPadButton(label = "v", onClick = {
            buttons = buttons xor ControllerButtons.DOWN
            onButtonsChange(buttons)
        })
    }
}

@Composable
private fun DPadButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
    ) {
        Text(label, fontSize = 8.sp, color = Color.Green)
    }
}

@Composable
private fun AnalogStickController(onMove: (x: Float, y: Float) -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .size(60.dp)
            .background(Color(0xFF1A1A1A))
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    offsetX = (offsetX + dragAmount.x / 30).coerceIn(-1f, 1f)
                    offsetY = (offsetY - dragAmount.y / 30).coerceIn(-1f, 1f)
                    onMove(offsetX, offsetY)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color.Green.copy(alpha = 0.5f), shape = CircleShape)
                .offset(x = (offsetX * 20).dp, y = (offsetY * 20).dp)
        )
    }
}

@Composable
private fun FaceButtonsController(onStateChange: (ControllerState) -> Unit) {
    var state by remember { mutableStateOf(ControllerState()) }

    Column(modifier = Modifier.size(70.dp)) {
        FaceButton(label = "T", color = Color(0xFFFF00FF), onClick = {
            state = state.copy(buttons = state.buttons xor ControllerButtons.TRIANGLE)
            onStateChange(state)
        })
        Row {
            FaceButton(label = "S", color = Color(0xFFFF00FF), onClick = {
                state = state.copy(buttons = state.buttons xor ControllerButtons.SQUARE)
                onStateChange(state)
            })
            FaceButton(label = "O", color = Color.Red, onClick = {
                state = state.copy(buttons = state.buttons xor ControllerButtons.CIRCLE)
                onStateChange(state)
            })
        }
        FaceButton(label = "X", color = Color.Blue, onClick = {
            state = state.copy(buttons = state.buttons xor ControllerButtons.CROSS)
            onStateChange(state)
        })
    }
}

@Composable
private fun FaceButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.3f))
    ) {
        Text(label, fontSize = 8.sp, color = color)
    }
}
