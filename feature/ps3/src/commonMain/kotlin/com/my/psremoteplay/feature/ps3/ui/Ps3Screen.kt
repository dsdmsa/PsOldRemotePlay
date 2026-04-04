package com.my.psremoteplay.feature.ps3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psremoteplay.core.ui.components.LogPanel
import com.my.psremoteplay.core.ui.components.VideoSurface
import com.my.psremoteplay.feature.ps3.presentation.Ps3Intent
import com.my.psremoteplay.feature.ps3.presentation.Ps3State

@Composable
fun Ps3Screen(
    state: Ps3State,
    onIntent: (Ps3Intent) -> Unit,
    currentFrame: ImageBitmap? = null,
    modifier: Modifier = Modifier
) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                if (maxWidth > 800.dp) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Ps3ControlPanel(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.width(300.dp).fillMaxHeight().padding(end = 8.dp)
                        )
                        VideoSurface(
                            title = "PS3 Remote Play",
                            statusText = state.statusText,
                            packetCount = state.videoPacketCount,
                            currentFrame = currentFrame,
                            idleContent = { Ps3IdleContent(state.statusText) },
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 8.dp)
                        )
                        LogPanel(
                            logs = state.logs,
                            onCopy = { onIntent(Ps3Intent.CopyLogs) },
                            onClear = { onIntent(Ps3Intent.ClearLogs) },
                            modifier = Modifier.width(400.dp).fillMaxHeight()
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        VideoSurface(
                            title = "PS3 Remote Play",
                            statusText = state.statusText,
                            packetCount = state.videoPacketCount,
                            currentFrame = currentFrame,
                            idleContent = { Ps3IdleContent(state.statusText) },
                            modifier = Modifier.fillMaxWidth().weight(0.3f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Ps3ControlPanel(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.fillMaxWidth().weight(0.35f)
                        )
                        Spacer(Modifier.height(8.dp))
                        LogPanel(
                            logs = state.logs,
                            onCopy = { onIntent(Ps3Intent.CopyLogs) },
                            onClear = { onIntent(Ps3Intent.ClearLogs) },
                            modifier = Modifier.fillMaxWidth().weight(0.35f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Ps3IdleContent(statusText: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PS3 Remote Play", color = Color.Cyan, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.background(Color(0xFF1A1A1A)).padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Setup:", color = Color.Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text("1. Discover PS3 (broadcast or direct IP)", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("2. Enter PKey + Device ID + MAC", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("3. Click 'Connect Session'", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            Text("Or use xRegistry bypass to skip registration.", color = Color.Yellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
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
