package com.my.psoldremoteplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psoldremoteplay.presentation.*

@Composable
fun LogPanel(
    state: RemotePlayState,
    onIntent: (RemotePlayIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logs (${state.logs.size})", style = MaterialTheme.typography.titleMedium)
            Row {
                TextButton(onClick = { onIntent(RemotePlayIntent.CopyLogs) }) {
                    Text("Copy All", fontSize = 11.sp)
                }
                TextButton(onClick = { onIntent(RemotePlayIntent.ClearLogs) }) {
                    Text("Clear", fontSize = 11.sp)
                }
            }
        }

        val scrollState = rememberScrollState()
        val logText = remember(state.logs.size) {
            if (state.logs.isEmpty()) {
                "Logs will appear here...\n\nTip: You can select and copy any text."
            } else {
                state.logs.joinToString("\n") { entry ->
                    val prefix = if (entry.isError) "[ERROR:${entry.tag}]" else "[${entry.tag}]"
                    "[${entry.timestamp}]$prefix ${entry.message}"
                }
            }
        }

        LaunchedEffect(state.logs.size) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D0D1A))
                    .verticalScroll(scrollState)
                    .padding(6.dp)
            ) {
                Text(
                    text = logText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00FF88),
                    lineHeight = 14.sp
                )
            }
        }
    }
}
