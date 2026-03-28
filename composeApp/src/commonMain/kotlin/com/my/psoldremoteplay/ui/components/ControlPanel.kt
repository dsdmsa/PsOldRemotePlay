package com.my.psoldremoteplay.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psoldremoteplay.presentation.*

@Composable
fun ControlPanel(
    state: RemotePlayState,
    onIntent: (RemotePlayIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Controls", style = MaterialTheme.typography.titleMedium)
        Text("Status: ${state.statusText}", color = Color.Cyan, fontSize = 12.sp)
        Text("PS3: ${state.ps3Nickname.ifBlank { "Not discovered" }}", fontSize = 12.sp)
        if (state.ps3Mac.isNotBlank()) {
            Text("MAC: ${state.ps3Mac}", fontSize = 11.sp, color = Color.Gray)
        }

        // Broadcast discovery
        Button(
            onClick = { onIntent(RemotePlayIntent.DiscoverBroadcast) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.connectionStatus != ConnectionStatus.Discovering
        ) { Text("Discover PS3 (broadcast)") }

        HorizontalDivider()

        // Manual IP
        OutlinedTextField(
            value = state.ps3Ip,
            onValueChange = { onIntent(RemotePlayIntent.UpdateIp(it)) },
            label = { Text("PS3 IP Address") },
            placeholder = { Text("192.168.1.75") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
        )

        // Direct discovery
        Button(
            onClick = { onIntent(RemotePlayIntent.DiscoverDirect(state.ps3Ip)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.ps3Ip.isNotBlank() && state.connectionStatus != ConnectionStatus.Discovering
        ) { Text("Direct Discovery (to IP)") }

        HorizontalDivider()
        Text("Registration", style = MaterialTheme.typography.labelMedium, color = Color.Yellow)

        OutlinedTextField(
            value = state.pin,
            onValueChange = { onIntent(RemotePlayIntent.UpdatePin(it)) },
            label = { Text("8-digit PIN (from PS3 screen)") },
            placeholder = { Text("e.g. 46865823") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        )

        Button(
            onClick = { onIntent(RemotePlayIntent.Register) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.ps3Ip.isNotBlank() && state.pin.length == 8,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) { Text("Register Device (PIN from PS3)") }

        HorizontalDivider()
        Text("Session", style = MaterialTheme.typography.labelMedium, color = Color.Cyan)

        OutlinedTextField(
            value = state.pkey,
            onValueChange = { onIntent(RemotePlayIntent.UpdatePkey(it)) },
            label = { Text("PKey (32 hex chars)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        )
        OutlinedTextField(
            value = state.deviceId,
            onValueChange = { onIntent(RemotePlayIntent.UpdateDeviceId(it)) },
            label = { Text("Device ID (32 hex)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        )
        OutlinedTextField(
            value = state.deviceMac,
            onValueChange = { onIntent(RemotePlayIntent.UpdateDeviceMac(it)) },
            label = { Text("Device MAC (12 hex)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        )

        HorizontalDivider()

        Button(
            onClick = { onIntent(RemotePlayIntent.ConnectSession) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.ps3Ip.isNotBlank() && state.connectionStatus != ConnectionStatus.Connecting
        ) { Text("Connect Session") }

        Button(
            onClick = { onIntent(RemotePlayIntent.TestConnection) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333355))
        ) { Text("Test Connection (no keys)") }

        if (state.isStreaming) {
            Button(
                onClick = { onIntent(RemotePlayIntent.Disconnect) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
            ) { Text("Disconnect") }
        }

        Spacer(Modifier.height(8.dp))
        Text("Video packets: ${state.videoPacketCount}", fontSize = 11.sp, color = Color.Gray)
    }
}
