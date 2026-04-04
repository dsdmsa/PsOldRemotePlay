package com.my.psremoteplay.app.ps2client

import androidx.compose.runtime.*
import androidx.compose.ui.window.ComposeUIViewController
import com.my.psremoteplay.feature.ps2.di.IosPs2ClientDependencies
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientViewModel
import com.my.psremoteplay.feature.ps2.ui.Ps2ClientScreen

fun MainViewController() = ComposeUIViewController {
    val deps = remember { IosPs2ClientDependencies() }
    val viewModel = remember { Ps2ClientViewModel(deps) }
    val state by viewModel.state.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()
    Ps2ClientScreen(state = state, onIntent = viewModel::onIntent, currentFrame = currentFrame)
}
