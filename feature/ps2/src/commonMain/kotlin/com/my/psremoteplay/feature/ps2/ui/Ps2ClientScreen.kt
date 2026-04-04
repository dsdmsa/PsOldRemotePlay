package com.my.psremoteplay.feature.ps2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psremoteplay.core.streaming.ConnectionStatus
import com.my.psremoteplay.core.ui.components.LogPanel
import com.my.psremoteplay.core.ui.components.OnScreenController
import com.my.psremoteplay.core.ui.components.VideoSurface
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientIntent
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientState

@Composable
fun Ps2ClientScreen(
    state: Ps2ClientState,
    onIntent: (Ps2ClientIntent) -> Unit,
    currentFrame: ImageBitmap? = null,
    modifier: Modifier = Modifier
) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                if (maxWidth > 800.dp) {
                    // Wide layout: controls | video | logs
                    Row(modifier = Modifier.fillMaxSize()) {
                        Ps2ClientControlPanel(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.width(250.dp).fillMaxHeight().padding(end = 8.dp)
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 8.dp)) {
                            VideoSurface(
                                title = "PS2 Remote Play",
                                statusText = state.statusText,
                                packetCount = state.videoFrameCount,
                                currentFrame = currentFrame,
                                idleContent = { Ps2IdleContent(state.statusText) },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (state.isStreaming && state.showController) {
                                OnScreenController(
                                    onStateChange = { onIntent(Ps2ClientIntent.ControllerInput(it)) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        LogPanel(
                            logs = state.logs,
                            onCopy = { /* no copy intent on client -- clear only */ },
                            onClear = { onIntent(Ps2ClientIntent.ClearLogs) },
                            modifier = Modifier.width(350.dp).fillMaxHeight()
                        )
                    }
                } else {
                    // Narrow layout: video | controls | logs
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(0.4f)) {
                            VideoSurface(
                                title = "PS2 Remote Play",
                                statusText = state.statusText,
                                packetCount = state.videoFrameCount,
                                currentFrame = currentFrame,
                                idleContent = { Ps2IdleContent(state.statusText) },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (state.isStreaming && state.showController) {
                                OnScreenController(
                                    onStateChange = { onIntent(Ps2ClientIntent.ControllerInput(it)) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Ps2ClientControlPanel(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.fillMaxWidth().weight(0.3f)
                        )
                        Spacer(Modifier.height(8.dp))
                        LogPanel(
                            logs = state.logs,
                            onCopy = { /* no copy intent on client */ },
                            onClear = { onIntent(Ps2ClientIntent.ClearLogs) },
                            modifier = Modifier.fillMaxWidth().weight(0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Ps2ClientControlPanel(
    state: Ps2ClientState,
    onIntent: (Ps2ClientIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "PS2 Client",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace
        )

        Text(
            "Status: ${state.statusText}",
            color = Color.Cyan,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        HorizontalDivider()

        OutlinedTextField(
            value = state.serverIp,
            onValueChange = { onIntent(Ps2ClientIntent.UpdateServerIp(it)) },
            label = { Text("Server IP") },
            placeholder = { Text("192.168.1.100") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        )

        OutlinedTextField(
            value = state.serverPort.toString(),
            onValueChange = { text ->
                text.filter { it.isDigit() }.toIntOrNull()?.let { port ->
                    onIntent(Ps2ClientIntent.UpdateServerPort(port))
                }
            },
            label = { Text("Server Port") },
            placeholder = { Text("9295") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        )

        HorizontalDivider()

        Button(
            onClick = { onIntent(Ps2ClientIntent.Connect) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.serverIp.isNotBlank() &&
                    state.connectionStatus != ConnectionStatus.Connecting &&
                    state.connectionStatus != ConnectionStatus.Streaming,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) { Text("Connect") }

        Button(
            onClick = { onIntent(Ps2ClientIntent.Disconnect) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.connectionStatus != ConnectionStatus.Disconnected,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
        ) { Text("Disconnect") }

        if (state.isStreaming) {
            HorizontalDivider()
            Button(
                onClick = { onIntent(Ps2ClientIntent.ToggleController) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333355))
            ) {
                Text(if (state.showController) "Hide Controller" else "Show Controller")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (state.gameName.isNotBlank()) {
            Text(
                "Game: ${state.gameName}",
                fontSize = 11.sp,
                color = Color.Green,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            "Video frames: ${state.videoFrameCount}",
            fontSize = 11.sp,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun Ps2IdleContent(statusText: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "PS2 Remote Play",
            color = Color.Cyan,
            fontSize = 24.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.background(Color(0xFF1A1A1A)).padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "Enter server IP and connect",
                color = Color.Green,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "1. Start PS2 Streaming Server on your PC",
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "2. Enter the server's IP address",
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "3. Click 'Connect' to start streaming",
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            statusText,
            color = when {
                statusText.contains("Error") -> Color.Red
                statusText.contains("Streaming") -> Color.Green
                else -> Color.Cyan
            },
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
