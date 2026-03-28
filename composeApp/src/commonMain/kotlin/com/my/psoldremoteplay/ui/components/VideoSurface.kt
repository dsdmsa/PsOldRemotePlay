package com.my.psoldremoteplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psoldremoteplay.presentation.RemotePlayState

@Composable
fun VideoSurface(
    state: RemotePlayState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (state.videoPacketCount > 0) {
            Text(
                "Receiving video stream\n${state.videoPacketCount} packets",
                color = Color.Green,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Video Surface", color = Color.Gray, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Text("1. Discover or enter PS3 IP", color = Color.DarkGray, fontSize = 12.sp)
                Text("2. Enter PKey + Device ID + MAC", color = Color.DarkGray, fontSize = 12.sp)
                Text("3. Click Connect Session", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    }
}
