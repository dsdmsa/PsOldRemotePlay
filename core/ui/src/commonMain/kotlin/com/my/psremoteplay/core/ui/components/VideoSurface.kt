package com.my.psremoteplay.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VideoSurface(
    title: String,
    statusText: String,
    packetCount: Int,
    currentFrame: ImageBitmap? = null,
    idleContent: @Composable () -> Unit = { DefaultIdleContent(title, statusText) },
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (currentFrame != null) {
            Image(
                bitmap = currentFrame,
                contentDescription = "$title video stream",
                modifier = Modifier.fillMaxSize().padding(0.dp),
                contentScale = ContentScale.Fit
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        "Packets: $packetCount",
                        color = Color.Green,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        statusText,
                        color = Color.Cyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else if (packetCount > 0) {
            Box(
                modifier = Modifier.background(Color(0xFF1A1A1A)).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Receiving stream...",
                        color = Color.Green,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$packetCount packets received",
                        color = Color.Cyan,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Codec initializing...",
                        color = Color.Yellow,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            idleContent()
        }
    }
}

@Composable
private fun DefaultIdleContent(title: String, statusText: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            color = Color.Cyan,
            fontSize = 24.sp,
            fontFamily = FontFamily.Monospace
        )
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
