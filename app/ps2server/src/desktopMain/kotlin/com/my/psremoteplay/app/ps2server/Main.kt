package com.my.psremoteplay.app.ps2server

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.my.psremoteplay.feature.ps2.di.DesktopPs2ServerDependencies
import com.my.psremoteplay.feature.ps2.presentation.Ps2ServerEffect
import com.my.psremoteplay.feature.ps2.presentation.Ps2ServerViewModel
import com.my.psremoteplay.feature.ps2.strategy.StreamingPreset
import com.my.psremoteplay.feature.ps2.ui.Ps2ServerScreen
import kotlinx.coroutines.flow.collectLatest
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

fun main() = application {
    val deps = remember { DesktopPs2ServerDependencies(StreamingPreset.H264_HW) }
    val viewModel = remember { Ps2ServerViewModel(deps) }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is Ps2ServerEffect.CopyToClipboard -> {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(StringSelection(effect.text), null)
                }
                is Ps2ServerEffect.ShowMessage -> println(effect.message)
            }
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "PS2 Streaming Server",
        state = rememberWindowState(width = 900.dp, height = 600.dp)
    ) {
        val state by viewModel.state.collectAsState()
        Ps2ServerScreen(state = state, onIntent = viewModel::onIntent)
    }
}
