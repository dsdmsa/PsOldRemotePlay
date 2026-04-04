package com.my.psremoteplay.app.ps2client

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.my.psremoteplay.feature.ps2.di.DesktopPs2ClientDependencies
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientEffect
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientViewModel
import com.my.psremoteplay.feature.ps2.ui.Ps2ClientScreen
import kotlinx.coroutines.flow.collectLatest

fun main() = application {
    val deps = remember { DesktopPs2ClientDependencies() }
    val viewModel = remember { Ps2ClientViewModel(deps) }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is Ps2ClientEffect.ShowMessage -> println(effect.message)
            }
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "PS2 Remote Play",
        state = rememberWindowState(width = 1100.dp, height = 700.dp)
    ) {
        val state by viewModel.state.collectAsState()
        val currentFrame by viewModel.currentFrame.collectAsState()
        Ps2ClientScreen(state = state, onIntent = viewModel::onIntent, currentFrame = currentFrame)
    }
}
