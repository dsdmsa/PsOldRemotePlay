package com.my.psremoteplay.feature.ps2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.my.psremoteplay.feature.ps2.ui.upscale.SharpenMethod
import com.my.psremoteplay.feature.ps2.ui.upscale.UpscaleMethod

@Composable
fun UpscaleSettingsDrawer(
    upscaleMethod: UpscaleMethod,
    sharpenMethod: SharpenMethod,
    sharpness: Float,
    bitrateMbps: Int,
    onUpscaleMethodChanged: (UpscaleMethod) -> Unit,
    onSharpenMethodChanged: (SharpenMethod) -> Unit,
    onSharpnessChanged: (Float) -> Unit,
    onBitrateChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Gear button
        Button(
            onClick = { isOpen = !isOpen },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 8.dp).size(36.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOpen) Color.Cyan.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) { Text("\u2699", color = Color.White, fontSize = 18.sp) }

        // Scrim
        if (isOpen) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { isOpen = false })
        }

        // Drawer
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier
                    .width(280.dp).fillMaxHeight()
                    .background(Color(0x1A, 0x1A, 0x2E).copy(alpha = 0.95f), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Settings", color = Color.Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Button(onClick = { isOpen = false }, modifier = Modifier.size(28.dp), shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("\u2715", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp) }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Upscaler
                SectionLabel("Upscaler")
                UpscaleMethod.entries.forEach { method ->
                    OptionRow(method.displayName, method == upscaleMethod) { onUpscaleMethodChanged(method) }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Sharpener
                SectionLabel("Sharpener")
                SharpenMethod.entries.forEach { method ->
                    OptionRow(method.displayName, method == sharpenMethod) { onSharpenMethodChanged(method) }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Sharpness
                val sharpEnabled = sharpenMethod != SharpenMethod.NONE
                SectionLabel("Sharpness", enabled = sharpEnabled)
                SliderRow(sharpness, 0f, 1f, sharpEnabled, onSharpnessChanged, "%.2f".format(sharpness))

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Bitrate
                SectionLabel("Bitrate")
                SliderRow(bitrateMbps.toFloat(), 2f, 30f, true, { onBitrateChanged(it.toInt()) }, "${bitrateMbps} Mbps")

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Pipeline info
                Box(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(10.dp)) {
                    Text(
                        "${upscaleMethod.displayName} → ${sharpenMethod.displayName}",
                        color = Color.Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, enabled: Boolean = true) {
    Text(text, color = if (enabled) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
        fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color.Cyan.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected, onClick, colors = RadioButtonDefaults.colors(
            selectedColor = Color.Cyan, unselectedColor = Color.White.copy(alpha = 0.4f)
        ), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SliderRow(value: Float, min: Float, max: Float, enabled: Boolean, onChange: (Float) -> Unit, label: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Slider(value, onChange, valueRange = min..max, enabled = enabled, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.Cyan, activeTrackColor = Color.Cyan.copy(alpha = 0.6f),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                disabledThumbColor = Color.Gray.copy(alpha = 0.4f),
                disabledActiveTrackColor = Color.Gray.copy(alpha = 0.2f)
            ))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (enabled) Color.Cyan else Color.Gray.copy(alpha = 0.4f),
            fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
