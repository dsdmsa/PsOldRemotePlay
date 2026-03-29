package com.my.psoldremoteplay.ui

import androidx.compose.foundation.background
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

/**
 * Settings screen for PS3 Remote Play client.
 *
 * Features:
 * - Video quality selection (bitrate presets)
 * - Platform type (PSP/Phone/PC)
 * - Connection history
 * - xRegistry bypass helper
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedQuality by remember { mutableStateOf("512 kbps") }
    var selectedPlatform by remember { mutableStateOf("Phone") }

    val qualityOptions = listOf("256 kbps", "384 kbps", "512 kbps", "768 kbps", "1024 kbps")
    val platformOptions = listOf("PSP", "Phone", "PC")

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = modifier.fillMaxSize().background(Color.Black)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "⚙ Settings",
                        color = Color.Cyan,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onBackClick,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33333333))
                    ) {
                        Text("Back", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Video Quality Section
                SettingSection(
                    title = "Video Quality",
                    description = "Higher quality requires more bandwidth"
                ) {
                    qualityOptions.forEach { option ->
                        SettingRadioButton(
                            label = option,
                            selected = selectedQuality == option,
                            onSelect = { selectedQuality = option }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Platform Type Section
                SettingSection(
                    title = "Device Type",
                    description = "PS3 menu will show this device type"
                ) {
                    platformOptions.forEach { option ->
                        SettingRadioButton(
                            label = option,
                            selected = selectedPlatform == option,
                            onSelect = { selectedPlatform = option }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Info Section
                SettingSection(title = "About") {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "PS3 Remote Play Client",
                            color = Color.Green,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "PREMO Protocol - Reverse Engineered",
                            color = Color.DarkGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Session: Fully implemented ✓",
                            color = Color.Green,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Video: Transport ready (decode pending)",
                            color = Color.Yellow,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Controller: Protocol ready (TCP batching pending)",
                            color = Color.Yellow,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Debug Section
                SettingSection(title = "Debug") {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Current Settings:",
                            color = Color.Cyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Quality: $selectedQuality",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Platform: $selectedPlatform",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1a1a1a))
            .padding(12.dp)
    ) {
        Text(
            title,
            color = Color.Green,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
        if (description != null) {
            Text(
                description,
                color = Color.DarkGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
}

@Composable
private fun SettingRadioButton(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.Green,
                unselectedColor = Color.DarkGray
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (selected) Color.Green else Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
