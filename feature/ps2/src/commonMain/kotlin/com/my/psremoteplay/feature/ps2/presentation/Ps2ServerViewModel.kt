package com.my.psremoteplay.feature.ps2.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.psremoteplay.core.streaming.LogEntry
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.di.Ps2ServerDependencies
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class Ps2ServerViewModel(private val deps: Ps2ServerDependencies) : ViewModel() {

    private val _state = MutableStateFlow(Ps2ServerState())
    val state: StateFlow<Ps2ServerState> = _state.asStateFlow()

    private val _effects = Channel<Ps2ServerEffect>(Channel.BUFFERED)
    val effects: Flow<Ps2ServerEffect> = _effects.receiveAsFlow()

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

    fun onIntent(intent: Ps2ServerIntent) {
        when (intent) {
            is Ps2ServerIntent.UpdatePcsx2Path -> updateState { copy(pcsx2Path = intent.path) }
            is Ps2ServerIntent.UpdateGamePath -> updateState { copy(gamePath = intent.path) }
            is Ps2ServerIntent.UpdatePort -> updateState { copy(serverPort = intent.port) }
            is Ps2ServerIntent.StartAll -> startAll()
            is Ps2ServerIntent.StopAll -> stopAll()
            is Ps2ServerIntent.ClearLogs -> updateState { copy(logs = emptyList()) }
            is Ps2ServerIntent.CopyLogs -> copyLogs()
        }
    }

    private var previousButtons = 0

    private fun startAll() {
        val s = _state.value
        if (s.pcsx2Path.isBlank() || s.gamePath.isBlank()) {
            logger.error("SERVER", "Set PCSX2 path and game path first")
            return
        }

        viewModelScope.launch {
            logger.log("SERVER", "=== STARTING ALL ===")
            updateState { copy(statusText = "Launching PCSX2...") }

            // 1. Launch PCSX2
            val launched = deps.launchEmulator(s.pcsx2Path, s.gamePath)
            if (!launched) {
                logger.error("SERVER", "Failed to launch PCSX2")
                updateState { copy(statusText = "PCSX2 launch failed") }
                return@launch
            }
            updateState { copy(isPcsx2Running = true, statusText = "PCSX2 running, waiting 3s...") }

            // 2. Wait for PCSX2 to start rendering
            kotlinx.coroutines.delay(3000)

            // 3. Start control server first (TCP) — need client IP for video targeting
            logger.log("SERVER", "Starting control server on port ${s.serverPort}...")
            deps.startControlServer(s.serverPort)
            updateState { copy(isServerRunning = true, statusText = "Waiting for client...") }

            // 4. Register controller input handler
            deps.onClientInput { controllerState ->
                handleControllerInput(controllerState)
                _state.update { it.copy(inputsReceived = it.inputsReceived + 1) }
            }

            // 5. Wait for a client to connect, then get its IP for video targeting
            logger.log("SERVER", "Waiting for client to connect...")
            var clientIp: String? = null
            while (clientIp == null && deps.isControlServerRunning()) {
                clientIp = deps.getLastClientIp()
                if (clientIp == null) kotlinx.coroutines.delay(500)
            }
            if (clientIp == null) return@launch

            // 6. Start video stream targeting the client's IP
            val videoStrategy = deps.videoStreamServer
            val config = deps.streamConfig.copy(
                videoPort = s.serverPort + 1,
                targetIp = clientIp
            )
            logger.log("SERVER", "Starting video [${videoStrategy.name}] → $clientIp:${config.videoPort}...")
            updateState { copy(statusText = "Starting ${videoStrategy.name}...") }
            videoStrategy.start(config)
            updateState { copy(isCapturing = true, statusText = "Streaming to $clientIp") }

            // 7. Poll client count
            launch {
                while (deps.isControlServerRunning()) {
                    updateState { copy(connectedClients = deps.getClientCount()) }
                    kotlinx.coroutines.delay(1000)
                }
            }

            logger.log("SERVER", "=== ALL SYSTEMS RUNNING ===")
        }
    }

    private fun handleControllerInput(state: com.my.psremoteplay.core.streaming.input.ControllerState) {
        val newButtons = state.buttons
        val changed = newButtons xor previousButtons

        // Press newly pressed buttons
        for (bit in listOf(0x0001, 0x0002, 0x0004, 0x0008, 0x0010, 0x0020, 0x0040, 0x0080,
            0x0100, 0x0200, 0x0400, 0x0800, 0x1000, 0x2000, 0x4000, 0x8000)) {
            if (changed and bit != 0) {
                if (newButtons and bit != 0) deps.injectButtonPress(bit)
                else deps.injectButtonRelease(bit)
            }
        }
        previousButtons = newButtons

        // Handle analog sticks (digital threshold)
        deps.injectStickState(state)
    }

    private fun stopAll() {
        viewModelScope.launch {
            logger.log("SERVER", "=== STOPPING ALL ===")
            deps.stopControlServer()
            deps.videoStreamServer.stop()
            deps.stopEmulator()
            previousButtons = 0
            updateState {
                copy(
                    isServerRunning = false,
                    isPcsx2Running = false,
                    isCapturing = false,
                    connectedClients = 0,
                    statusText = "Stopped"
                )
            }
            logger.log("SERVER", "All stopped")
        }
    }

    private fun copyLogs() {
        val text = _state.value.logs.joinToString("\n") { entry ->
            val prefix = if (entry.isError) "[ERROR:${entry.tag}]" else "[${entry.tag}]"
            "[${entry.timestamp}]$prefix ${entry.message}"
        }
        viewModelScope.launch {
            _effects.send(Ps2ServerEffect.CopyToClipboard(text))
        }
    }

    private fun addLog(tag: String, message: String, isError: Boolean) {
        val entry = LogEntry(
            timestamp = currentTimestamp(),
            tag = tag,
            message = message,
            isError = isError
        )
        updateState { copy(logs = logs + entry) }
    }

    private inline fun updateState(transform: Ps2ServerState.() -> Ps2ServerState) {
        _state.update(transform)
    }
}

private fun currentTimestamp(): String {
    return SimpleDateFormat("HH:mm:ss.SSS").format(Date())
}
