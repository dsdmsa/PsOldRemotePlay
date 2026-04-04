package com.my.psremoteplay.core.ui.components

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
import com.my.psremoteplay.core.streaming.LogEntry

@Composable
fun LogPanel(
    logs: List<LogEntry>,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logs (${logs.size})", style = MaterialTheme.typography.titleMedium)
            Row {
                TextButton(onClick = onCopy) {
                    Text("Copy All", fontSize = 11.sp)
                }
                TextButton(onClick = onClear) {
                    Text("Clear", fontSize = 11.sp)
                }
            }
        }

        val scrollState = rememberScrollState()
        val logText = remember(logs.size) {
            if (logs.isEmpty()) {
                "Logs will appear here...\n\nTip: You can select and copy any text."
            } else {
                logs.joinToString("\n") { entry ->
                    val prefix = if (entry.isError) "[ERROR:${entry.tag}]" else "[${entry.tag}]"
                    "[${entry.timestamp}]$prefix ${entry.message}"
                }
            }
        }

        LaunchedEffect(logs.size) {
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
