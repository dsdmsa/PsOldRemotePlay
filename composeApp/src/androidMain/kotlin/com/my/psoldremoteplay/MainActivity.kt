package com.my.psoldremoteplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.my.psoldremoteplay.di.AndroidDependencies
import com.my.psoldremoteplay.presentation.RemotePlayEffect
import com.my.psoldremoteplay.presentation.RemotePlayViewModel
import com.my.psoldremoteplay.ui.RemotePlayScreen
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val deps = AndroidDependencies()
        val viewModel = RemotePlayViewModel(deps)

        setContent {
            LaunchedEffect(Unit) {
                viewModel.effects.collectLatest { effect ->
                    when (effect) {
                        is RemotePlayEffect.CopyToClipboard -> {
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("logs", effect.text))
                        }
                        is RemotePlayEffect.ShowMessage -> {
                            android.widget.Toast.makeText(this@MainActivity, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            val state by viewModel.state.collectAsState()
            RemotePlayScreen(state = state, onIntent = viewModel::onIntent)
        }
    }
}
