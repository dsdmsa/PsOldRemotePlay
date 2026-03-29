package com.my.psoldremoteplay

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.ComposeUIViewController
import com.my.psoldremoteplay.di.IosDependencies
import com.my.psoldremoteplay.presentation.RemotePlayViewModel
import com.my.psoldremoteplay.ui.RemotePlayScreen

fun MainViewController() = ComposeUIViewController {
    val deps = IosDependencies()
    val viewModel = RemotePlayViewModel(deps)
    val state = viewModel.state.collectAsState().value
    RemotePlayScreen(state = state, onIntent = viewModel::onIntent)
}