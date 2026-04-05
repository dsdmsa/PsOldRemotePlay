package com.my.psremoteplay.app.ps2client

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.my.psremoteplay.feature.ps2.di.AndroidPs2ClientDependencies
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientEffect
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientIntent
import com.my.psremoteplay.feature.ps2.presentation.Ps2ClientViewModel
import com.my.psremoteplay.feature.ps2.ui.Ps2AndroidGameScreen
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Force landscape + keep screen on
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

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

            // Set LAN IP and auto-connect
            LaunchedEffect(Unit) {
                viewModel.onIntent(Ps2ClientIntent.UpdateServerIp("192.168.1.128"))
                viewModel.onIntent(Ps2ClientIntent.Connect)
            }

            val state by viewModel.state.collectAsState()
            val currentFrame by viewModel.currentFrame.collectAsState()

            Ps2AndroidGameScreen(
                currentFrame = currentFrame,
                onControllerState = { viewModel.onIntent(Ps2ClientIntent.ControllerInput(it)) },
                onClose = { finish() },
                statusText = state.statusText,
                frameCount = state.videoFrameCount
            )
        }
    }
}
