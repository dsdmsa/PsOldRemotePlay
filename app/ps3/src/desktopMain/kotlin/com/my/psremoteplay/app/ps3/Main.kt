package com.my.psremoteplay.app.ps3

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.my.psremoteplay.feature.ps3.di.DesktopPs3Dependencies
import com.my.psremoteplay.feature.ps3.presentation.Ps3Effect
import com.my.psremoteplay.feature.ps3.presentation.Ps3ViewModel
import com.my.psremoteplay.feature.ps3.ui.Ps3Screen
import kotlinx.coroutines.flow.collectLatest
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

fun main() = application {
    val deps = remember { DesktopPs3Dependencies() }
    val viewModel = remember { Ps3ViewModel(deps) }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is Ps3Effect.CopyToClipboard -> {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(StringSelection(effect.text), null)
                }
                is Ps3Effect.ShowMessage -> {
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
        val currentFrame by deps.streaming.videoRenderer.currentFrame.collectAsState()
        Ps3Screen(state = state, onIntent = viewModel::onIntent, currentFrame = currentFrame)
    }
}
