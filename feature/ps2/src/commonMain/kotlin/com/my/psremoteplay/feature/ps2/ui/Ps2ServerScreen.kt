package com.my.psremoteplay.feature.ps2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.my.psremoteplay.core.ui.components.LogPanel
import com.my.psremoteplay.feature.ps2.presentation.Ps2ServerIntent
import com.my.psremoteplay.feature.ps2.presentation.Ps2ServerState

@Composable
fun Ps2ServerScreen(
    state: Ps2ServerState,
    onIntent: (Ps2ServerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                if (maxWidth > 800.dp) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Ps2ServerControlPanel(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.width(300.dp).fillMaxHeight().padding(end = 8.dp)
                        )
                        LogPanel(
                            logs = state.logs,
                            onCopy = { onIntent(Ps2ServerIntent.CopyLogs) },
                            onClear = { onIntent(Ps2ServerIntent.ClearLogs) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Ps2ServerControlPanel(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.fillMaxWidth().weight(0.5f)
                        )
                        Spacer(Modifier.height(8.dp))
                        LogPanel(
                            logs = state.logs,
                            onCopy = { onIntent(Ps2ServerIntent.CopyLogs) },
                            onClear = { onIntent(Ps2ServerIntent.ClearLogs) },
                            modifier = Modifier.fillMaxWidth().weight(0.5f)
                        )
                    }
                }
            }
        }
    }
}
