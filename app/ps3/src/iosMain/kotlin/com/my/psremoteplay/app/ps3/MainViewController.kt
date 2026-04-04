package com.my.psremoteplay.app.ps3

import androidx.compose.ui.window.ComposeUIViewController
import com.my.psremoteplay.feature.ps3.di.IosPs3Dependencies
import com.my.psremoteplay.feature.ps3.presentation.Ps3ViewModel
import com.my.psremoteplay.feature.ps3.ui.Ps3Screen
import androidx.compose.runtime.*

fun MainViewController() = ComposeUIViewController {
    val deps = remember { IosPs3Dependencies() }
    val viewModel = remember { Ps3ViewModel(deps) }
    val state by viewModel.state.collectAsState()
    val currentFrame by deps.streaming.videoRenderer.currentFrame.collectAsState()
    Ps3Screen(state = state, onIntent = viewModel::onIntent, currentFrame = currentFrame)
}
