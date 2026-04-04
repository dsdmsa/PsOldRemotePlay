package com.my.psremoteplay.app.ps2client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.my.psremoteplay.feature.ps2.di.AndroidPs2ClientDependencies
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientEffect
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientViewModel
import com.my.psremoteplay.feature.ps2.ui.Ps2ClientScreen
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val deps = remember { AndroidPs2ClientDependencies() }
            val viewModel = viewModel { Ps2ClientViewModel(deps) }
            LaunchedEffect(Unit) {
                viewModel.effects.collectLatest { effect ->
                    when (effect) {
                        is Ps2ClientEffect.ShowMessage -> {
                            android.widget.Toast.makeText(this@MainActivity, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            val state by viewModel.state.collectAsState()
            val currentFrame by viewModel.currentFrame.collectAsState()
            Ps2ClientScreen(state = state, onIntent = viewModel::onIntent, currentFrame = currentFrame)
        }
    }
}
