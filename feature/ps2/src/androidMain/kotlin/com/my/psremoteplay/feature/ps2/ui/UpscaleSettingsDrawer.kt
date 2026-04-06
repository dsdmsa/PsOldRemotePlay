package com.my.psremoteplay.feature.ps2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.my.psremoteplay.feature.ps2.ui.upscale.UpscalePreset

/**
 * Right-side sliding drawer for upscaler settings.
 * Renders a gear toggle button and an overlay panel with preset selection and sharpness control.
 */
@Composable
fun UpscaleSettingsDrawer(
    currentPreset: UpscalePreset,
    sharpness: Float,
    onPresetChanged: (UpscalePreset) -> Unit,
    onSharpnessChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Gear toggle button (always visible, top-right area below close/reconnect row)
        Button(
            onClick = { isOpen = !isOpen },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 8.dp)
                .size(36.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOpen) Color.Cyan.copy(alpha = 0.6f)
                else Color.White.copy(alpha = 0.25f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("\u2699", color = Color.White, fontSize = 18.sp)
        }

        // Scrim: tap outside drawer to close
        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isOpen = false }
            )
        }

        // Sliding drawer panel
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(
                        Color(0xE0, 0x1A, 0x1A, 0x2E).copy(alpha = 0.95f),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Upscaler",
                        color = Color.Cyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Button(
                        onClick = { isOpen = false },
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("\u2715", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Preset selection
                Text(
                    "Preset",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )

                UpscalePreset.entries.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (preset == currentPreset) Color.Cyan.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { onPresetChanged(preset) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preset == currentPreset,
                            onClick = { onPresetChanged(preset) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.Cyan,
                                unselectedColor = Color.White.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            preset.displayName,
                            color = if (preset == currentPreset) Color.White
                            else Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Sharpness slider
                val sharpnessEnabled = currentPreset != UpscalePreset.NONE

                Text(
                    "Sharpness",
                    color = if (sharpnessEnabled) Color.White.copy(alpha = 0.6f)
                    else Color.White.copy(alpha = 0.25f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = sharpness,
                        onValueChange = onSharpnessChanged,
                        valueRange = 0f..1f,
                        enabled = sharpnessEnabled,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Cyan,
                            activeTrackColor = Color.Cyan.copy(alpha = 0.6f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                            disabledThumbColor = Color.Gray.copy(alpha = 0.4f),
                            disabledActiveTrackColor = Color.Gray.copy(alpha = 0.2f),
                            disabledInactiveTrackColor = Color.White.copy(alpha = 0.05f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "%.2f".format(sharpness),
                        color = if (sharpnessEnabled) Color.Cyan else Color.Gray.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Info text: active preset description
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        currentPreset.displayName,
                        color = Color.Cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        currentPreset.description,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
