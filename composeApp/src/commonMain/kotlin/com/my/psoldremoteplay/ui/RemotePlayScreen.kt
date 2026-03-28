package com.my.psoldremoteplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.my.psoldremoteplay.presentation.*
import com.my.psoldremoteplay.ui.components.*

@Composable
fun RemotePlayScreen(
    state: RemotePlayState,
    onIntent: (RemotePlayIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                if (maxWidth > 800.dp) {
                    // Wide layout: Controls | Video | Logs
                    Row(modifier = Modifier.fillMaxSize()) {
                        ControlPanel(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.width(300.dp).fillMaxHeight().padding(end = 8.dp)
                        )
                        VideoSurface(
                            state = state,
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 8.dp)
                        )
                        LogPanel(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.width(400.dp).fillMaxHeight()
                        )
                    }
                } else {
                    // Narrow layout: Video on top, Controls + Logs below
                    Column(modifier = Modifier.fillMaxSize()) {
                        VideoSurface(
                            state = state,
                            modifier = Modifier.fillMaxWidth().weight(0.3f)
                        )
                        Spacer(Modifier.height(8.dp))
                        ControlPanel(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.fillMaxWidth().weight(0.35f)
                        )
                        Spacer(Modifier.height(8.dp))
                        LogPanel(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.fillMaxWidth().weight(0.35f)
                        )
                    }
                }
            }
        }
    }
}
