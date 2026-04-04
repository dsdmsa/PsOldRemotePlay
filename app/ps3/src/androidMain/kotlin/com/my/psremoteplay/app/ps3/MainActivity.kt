package com.my.psremoteplay.app.ps3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.my.psremoteplay.feature.ps3.di.AndroidPs3Dependencies
import com.my.psremoteplay.feature.ps3.presentation.Ps3Effect
import com.my.psremoteplay.feature.ps3.presentation.Ps3ViewModel
import com.my.psremoteplay.feature.ps3.ui.Ps3Screen
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val deps = remember { AndroidPs3Dependencies() }
            val viewModel = viewModel { Ps3ViewModel(deps) }

            LaunchedEffect(Unit) {
                viewModel.effects.collectLatest { effect ->
                    when (effect) {
                        is Ps3Effect.CopyToClipboard -> {
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("logs", effect.text))
                        }
                        is Ps3Effect.ShowMessage -> {
                            android.widget.Toast.makeText(this@MainActivity, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            val state by viewModel.state.collectAsState()
            val currentFrame by deps.streaming.videoRenderer.currentFrame.collectAsState()
            Ps3Screen(state = state, onIntent = viewModel::onIntent, currentFrame = currentFrame)
        }
    }
}
