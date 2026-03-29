package com.my.psoldremoteplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psoldremoteplay.presentation.RemotePlayState
import androidx.compose.foundation.Image

/**
 * Video display surface.
 * Shows decoded video frames from the remote PS3, or helpful status/instructions.
 * Aspect ratio: 480:272 = 30:17 (letterboxed as needed)
 */
@Composable
fun VideoSurface(
    state: RemotePlayState,
    currentFrame: androidx.compose.ui.graphics.ImageBitmap? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (currentFrame != null) {
            // Display actual decoded video frame
            Image(
                bitmap = currentFrame,
                contentDescription = "PS3 video stream",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
                contentScale = ContentScale.Fit
            )

            // Stats overlay (bottom-right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        "Packets: ${state.videoPacketCount}",
                        color = Color.Green,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        state.statusText,
                        color = Color.Cyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else if (state.videoPacketCount > 0) {
            // Video stream active but no decoded frame yet (codec loading or placeholder)
            Box(
                modifier = Modifier
                    .background(Color(0x1a1a1a))
                    .fillMaxSize(),
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
                        "${state.videoPacketCount} packets received",
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
            // Not streaming yet
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "PS3 Remote Play",
                    color = Color.Cyan,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .background(Color(0x1a1a1a))
                        .padding(16.dp),
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
                    state.statusText,
                    color = when {
                        state.statusText.contains("Error") -> Color.Red
                        state.statusText.contains("Streaming") -> Color.Green
                        else -> Color.Cyan
                    },
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
