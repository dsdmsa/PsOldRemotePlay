package com.my.psoldremoteplay

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.my.psoldremoteplay.di.DesktopDependencies
import com.my.psoldremoteplay.presentation.RemotePlayEffect
import com.my.psoldremoteplay.presentation.RemotePlayViewModel
import com.my.psoldremoteplay.ui.RemotePlayScreen
import kotlinx.coroutines.flow.collectLatest
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

fun main() = application {
    val deps = remember { DesktopDependencies() }
    val viewModel = remember { RemotePlayViewModel(deps) }

    // Handle effects (clipboard, etc.)
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is RemotePlayEffect.CopyToClipboard -> {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(StringSelection(effect.text), null)
                }
                is RemotePlayEffect.ShowMessage -> {
                    println(effect.message)
                }
            }
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "PS3 Remote Play",
        state = rememberWindowState(width = 1200.dp, height = 750.dp)
    ) {
        val state by viewModel.state.collectAsState()
        val currentFrame by deps.videoRenderer.currentFrame.collectAsState()
        RemotePlayScreen(state = state, onIntent = viewModel::onIntent, currentFrame = currentFrame)
    }
}
