package com.my.psremoteplay.feature.ps2.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.psremoteplay.core.streaming.ConnectionStatus
import com.my.psremoteplay.core.streaming.LogEntry
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.di.Ps2ClientDependencies
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class Ps2ClientViewModel(private val deps: Ps2ClientDependencies) : ViewModel() {

    private val _state = MutableStateFlow(Ps2ClientState())
    val state: StateFlow<Ps2ClientState> = _state.asStateFlow()

    /** Video frames from the active strategy */
    val currentFrame: StateFlow<ImageBitmap?> = deps.videoStreamClient.currentFrame

    private val _effects = Channel<Ps2ClientEffect>(Channel.BUFFERED)
    val effects: Flow<Ps2ClientEffect> = _effects.receiveAsFlow()

    private var inputCount = 0L

    private val logger = object : Logger {
        override fun log(tag: String, message: String) {
            addLog(tag, message, isError = false)
            deps.logger.log(tag, message)
        }
        override fun error(tag: String, message: String, throwable: Throwable?) {
            val errMsg = "$message${throwable?.let { "\n  ${it::class.simpleName}: ${it.message}" } ?: ""}"
            addLog(tag, errMsg, isError = true)
            deps.logger.error(tag, message, throwable)
        }
    }

    fun onIntent(intent: Ps2ClientIntent) {
        when (intent) {
            is Ps2ClientIntent.UpdateServerIp -> updateState { copy(serverIp = sanitizeHost(intent.ip)) }
            is Ps2ClientIntent.UpdateServerPort -> updateState { copy(serverPort = intent.port) }
            is Ps2ClientIntent.Connect -> connect()
            is Ps2ClientIntent.Disconnect -> disconnect()
            is Ps2ClientIntent.ControllerInput -> handleControllerInput(intent.state)
            is Ps2ClientIntent.ToggleController -> updateState { copy(showController = !showController) }
            is Ps2ClientIntent.ClearLogs -> updateState { copy(logs = emptyList()) }
        }
    }

    private fun connect() {
        val s = _state.value
        if (s.serverIp.isBlank()) {
            logger.error("CLIENT", "Enter server IP first")
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val strategy = deps.videoStreamClient
            val config = deps.streamConfig.copy(videoPort = s.serverPort + 1)

            logger.log("CLIENT", "Connecting [${strategy.name}] to ${s.serverIp}...")
            updateState { copy(connectionStatus = ConnectionStatus.Connecting, statusText = "Connecting...") }

            // 1. Connect control channel (TCP)
            val controlOk = deps.connectControl(s.serverIp, s.serverPort,
                onServerInfo = { info -> logger.log("CLIENT", "Server info: $info") }
            )
            if (!controlOk) {
                updateState { copy(connectionStatus = ConnectionStatus.Error, statusText = "Control failed") }
                logger.error("CLIENT", "Control channel failed")
                return@launch
            }

            // 2. Connect video stream via strategy
            val videoOk = strategy.start(s.serverIp, config)
            if (!videoOk) {
                deps.disconnectControl()
                updateState { copy(connectionStatus = ConnectionStatus.Error, statusText = "Video failed") }
                logger.error("CLIENT", "Video stream failed")
                return@launch
            }

            // 3. Count frames (Surface-based clients need polling; ImageBitmap clients use flow)
            if (strategy.usesSurfaceRendering) {
                launch {
                    while (true) {
                        kotlinx.coroutines.delay(500)
                        _state.update { it.copy(videoFrameCount = strategy.decodedFrameCount.toInt()) }
                    }
                }
            } else {
                launch {
                    strategy.currentFrame.collect { frame ->
                        if (frame != null) _state.update { it.copy(videoFrameCount = it.videoFrameCount + 1) }
                    }
                }
            }

            updateState {
                copy(
                    connectionStatus = ConnectionStatus.Streaming,
                    isStreaming = true,
                    usesSurfaceRendering = strategy.usesSurfaceRendering,
                    statusText = "Streaming [${strategy.name}] from ${s.serverIp}"
                )
            }
            logger.log("CLIENT", "Connected via ${strategy.name}")
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            deps.videoStreamClient.stop()
            deps.disconnectControl()
            updateState {
                copy(
                    connectionStatus = ConnectionStatus.Disconnected,
                    isStreaming = false,
                    videoFrameCount = 0,
                    statusText = "Disconnected"
                )
            }
            logger.log("CLIENT", "Disconnected")
        }
    }

    private fun handleControllerInput(state: com.my.psremoteplay.core.streaming.input.ControllerState) {
        inputCount++
        if (deps.isConnected()) deps.sendControllerState(state)
        if (inputCount % 60 == 0L) logger.log("INPUT", "Controller input #$inputCount")
    }

    private fun addLog(tag: String, message: String, isError: Boolean) {
        val entry = LogEntry(timestamp = currentTimestamp(), tag = tag, message = message, isError = isError)
        updateState { copy(logs = logs + entry) }
    }

    private inline fun updateState(transform: Ps2ClientState.() -> Ps2ClientState) {
        _state.update(transform)
    }
}

private fun sanitizeHost(input: String): String {
    return input.trim()
        .removePrefix("http://").removePrefix("https://")
        .removeSuffix("/").split(":").first()
}

private fun currentTimestamp(): String = SimpleDateFormat("HH:mm:ss.SSS").format(Date())
