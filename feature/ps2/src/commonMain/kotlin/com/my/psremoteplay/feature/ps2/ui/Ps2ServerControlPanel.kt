package com.my.psremoteplay.feature.ps2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psremoteplay.feature.ps2.presentation.Ps2ServerIntent
import com.my.psremoteplay.feature.ps2.presentation.Ps2ServerState

@Composable
fun Ps2ServerControlPanel(
    state: Ps2ServerState,
    onIntent: (Ps2ServerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "PS2 Streaming Server",
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

        // Stats
        Text("Stats", style = MaterialTheme.typography.labelMedium, color = Color.Green)
        Text(
            "Clients connected: ${state.connectedClients}",
            fontSize = 11.sp,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "Frames sent: ${state.framesSent}",
            fontSize = 11.sp,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Text(
            "Inputs received: ${state.inputsReceived}",
            fontSize = 11.sp,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )

        HorizontalDivider()

        // Configuration
        Text("Configuration", style = MaterialTheme.typography.labelMedium, color = Color.Cyan)

        OutlinedTextField(
            value = state.pcsx2Path,
            onValueChange = { onIntent(Ps2ServerIntent.UpdatePcsx2Path(it)) },
            label = { Text("PCSX2 Path") },
            placeholder = { Text("/path/to/pcsx2") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        )

        OutlinedTextField(
            value = state.gamePath,
            onValueChange = { onIntent(Ps2ServerIntent.UpdateGamePath(it)) },
            label = { Text("Game ISO/BIN Path") },
            placeholder = { Text("/path/to/game.iso") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        )

        OutlinedTextField(
            value = state.serverPort.toString(),
            onValueChange = { text ->
                text.filter { it.isDigit() }.toIntOrNull()?.let { port ->
                    onIntent(Ps2ServerIntent.UpdatePort(port))
                }
            },
            label = { Text("Server Port") },
            placeholder = { Text("9295") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        )

        HorizontalDivider()

        // Controls
        Button(
            onClick = { onIntent(Ps2ServerIntent.StartAll) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isServerRunning,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) { Text("Start All") }

        Button(
            onClick = { onIntent(Ps2ServerIntent.StopAll) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isServerRunning,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
        ) { Text("Stop All") }

        HorizontalDivider()

        // Status indicators
        Text("Services", style = MaterialTheme.typography.labelMedium, color = Color.Cyan)
        StatusIndicator(label = "PCSX2", isActive = state.isPcsx2Running)
        StatusIndicator(label = "Capturing", isActive = state.isCapturing)
        StatusIndicator(label = "Server", isActive = state.isServerRunning)
    }
}

@Composable
private fun StatusIndicator(label: String, isActive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (isActive) "\u25CF" else "\u25CB",
            color = if (isActive) Color.Green else Color.Red,
            fontSize = 14.sp
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive) Color.Green else Color.Gray,
            fontFamily = FontFamily.Monospace
        )
    }
}
