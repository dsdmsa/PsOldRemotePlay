package com.my.psremoteplay.feature.ps3.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.psremoteplay.core.streaming.ConnectionStatus
import com.my.psremoteplay.core.streaming.LogEntry
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps3.di.Ps3Dependencies
import com.my.psremoteplay.feature.ps3.protocol.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class Ps3ViewModel(private val deps: Ps3Dependencies) : ViewModel() {

    private val _state = MutableStateFlow(Ps3State())
    val state: StateFlow<Ps3State> = _state.asStateFlow()

    private val _effects = Channel<Ps3Effect>(Channel.BUFFERED)
    val effects: Flow<Ps3Effect> = _effects.receiveAsFlow()

    private val logger = object : Logger {
        override fun log(tag: String, message: String) {
            addLog(tag, message, isError = false)
            deps.streaming.logger.log(tag, message)
        }

        override fun error(tag: String, message: String, throwable: Throwable?) {
            val errMsg = "$message${throwable?.let { "\n  ${it::class.simpleName}: ${it.message}" } ?: ""}"
            addLog(tag, errMsg, isError = true)
            deps.streaming.logger.error(tag, message, throwable)
        }
    }

    fun onIntent(intent: Ps3Intent) {
        when (intent) {
            is Ps3Intent.UpdateIp -> updateState { copy(ps3Ip = cleanIp(intent.ip)) }
            is Ps3Intent.UpdatePkey -> updateState { copy(pkey = intent.pkey) }
            is Ps3Intent.UpdatePin -> updateState { copy(pin = intent.pin.filter { it.isDigit() }.take(8)) }
            is Ps3Intent.UpdateDeviceId -> updateState { copy(deviceId = intent.id) }
            is Ps3Intent.UpdateDeviceMac -> updateState { copy(deviceMac = intent.mac) }
            is Ps3Intent.DiscoverBroadcast -> discoverBroadcast()
            is Ps3Intent.DiscoverDirect -> discoverDirect(intent.ip)
            is Ps3Intent.ConnectSession -> connectSession()
            is Ps3Intent.TestConnection -> testConnection()
            is Ps3Intent.Register -> register()
            is Ps3Intent.Disconnect -> disconnect()
            is Ps3Intent.ClearLogs -> updateState { copy(logs = emptyList()) }
            is Ps3Intent.CopyLogs -> copyLogs()
        }
    }

    private fun discoverBroadcast() {
        viewModelScope.launch {
            updateState { copy(connectionStatus = ConnectionStatus.Discovering, statusText = "Discovering (broadcast)...") }
            logger.log("DISCOVERY", "Starting broadcast discovery...")

            val info = deps.discoverer.discover()
            if (info != null) {
                updateState {
                    copy(
                        ps3Ip = info.ip, ps3Nickname = info.nickname, ps3Mac = info.macString,
                        connectionStatus = ConnectionStatus.Disconnected, statusText = "Found: ${info.ip}"
                    )
                }
            } else {
                logger.log("DISCOVERY", "Broadcast failed. Try 'Direct Discovery' with the PS3 IP.")
                updateState { copy(connectionStatus = ConnectionStatus.Disconnected, statusText = "Not found — try direct") }
            }
        }
    }

    private fun discoverDirect(ip: String) {
        val cleanedIp = cleanIp(ip)
        if (cleanedIp.isBlank()) { logger.error("DISCOVERY", "Enter PS3 IP first"); return }
        viewModelScope.launch {
            updateState { copy(ps3Ip = cleanedIp, connectionStatus = ConnectionStatus.Discovering, statusText = "Direct discovery to $cleanedIp...") }
            logger.log("DISCOVERY", "Sending SRCH directly to $cleanedIp...")

            val info = deps.discoverer.discoverDirect(cleanedIp, timeoutMs = 3000)
            if (info != null) {
                updateState {
                    copy(ps3Nickname = info.nickname, ps3Mac = info.macString,
                        connectionStatus = ConnectionStatus.Disconnected, statusText = "Found: ${info.nickname}")
                }
            } else {
                logger.log("DISCOVERY", "No response from $cleanedIp. Is Remote Play enabled on PS3?")
                updateState { copy(connectionStatus = ConnectionStatus.Disconnected, statusText = "No response from $cleanedIp") }
            }
        }
    }

    private fun connectSession() {
        val s = _state.value
        val ip = cleanIp(s.ps3Ip)
        if (ip.isBlank()) { logger.error("SESSION", "Set PS3 IP first"); return }

        val pkeyBytes = s.pkey.hexToByteArrayOrNull()
        val idBytes = s.deviceId.hexToByteArrayOrNull()
        val macBytes = s.deviceMac.hexToByteArrayOrNull()

        if (pkeyBytes == null || pkeyBytes.size != 16) { logger.error("SESSION", "PKey must be 32 hex chars (16 bytes)"); return }
        if (idBytes == null || idBytes.size != 16) { logger.error("SESSION", "Device ID must be 32 hex chars"); return }
        if (macBytes == null || macBytes.size != 6) { logger.error("SESSION", "MAC must be 12 hex chars"); return }

        viewModelScope.launch {
            updateState { copy(connectionStatus = ConnectionStatus.Connecting, statusText = "Connecting to $ip...") }
            logger.log("SESSION", "Connecting to $ip:${PremoConstants.PORT}...")

            val config = SessionConfig(pkeyBytes, idBytes, macBytes)
            val result = deps.sessionHandler.createSession(ip, config)

            result.onSuccess { resp ->
                updateState {
                    copy(sessionId = resp.sessionId, connectionStatus = ConnectionStatus.Connected,
                        statusText = "Connected! ${resp.execMode} | ${resp.videoCodec} ${resp.videoResolution}")
                }
                logger.log("SESSION", "=== SESSION ESTABLISHED ===")
                logger.log("SESSION", "SessionID: ${resp.sessionId}")
                resp.rawHeaders.forEach { (k, v) -> logger.log("SESSION", "  $k: $v") }

                val authToken = generateAuthToken(deps.streaming.crypto, pkeyBytes, resp.nonce, macBytes)
                startVideoStream(ip, resp.sessionId, authToken, pkeyBytes, resp.nonce)
            }
            result.onFailure { e ->
                updateState { copy(connectionStatus = ConnectionStatus.Error, statusText = "Failed: ${e.message?.take(50)}") }
            }
        }
    }

    private fun startVideoStream(ip: String, sessionId: String, authToken: String, pkeyBytes: ByteArray, nonce: ByteArray) {
        viewModelScope.launch {
            updateState { copy(isStreaming = true, connectionStatus = ConnectionStatus.Streaming, statusText = "Streaming...") }

            val aesKey = xorBytes(pkeyBytes, PremoConstants.SKEY0)
            val aesIv = xorBytes(nonce, PremoConstants.SKEY2)

            deps.streaming.videoRenderer.start()
            logger.log("VIDEO", "Starting video stream...")
            deps.sessionHandler.startVideoStream(ip, sessionId, authToken, aesKey, aesIv) { packet ->
                try {
                    deps.streaming.videoRenderer.onStreamPacket(packet.rawHeader, packet.payload, false)
                    updateState { copy(videoPacketCount = videoPacketCount + 1) }
                } catch (e: Exception) {
                    logger.error("VIDEO", "Packet processing failed: ${e.message}")
                }
            }

            deps.streaming.audioRenderer.start()
            deps.sessionHandler.startAudioStream(ip, sessionId, authToken, aesKey, aesIv) { packet ->
                try {
                    logger.log("AUDIO", "Audio packet: ${packet.payload.size} bytes")
                } catch (e: Exception) {
                    logger.error("AUDIO", "Audio processing failed: ${e.message}")
                }
            }
        }
    }

    private fun testConnection() {
        val ip = cleanIp(_state.value.ps3Ip)
        if (ip.isBlank()) { logger.error("TEST", "Set PS3 IP first"); return }

        viewModelScope.launch {
            updateState { copy(statusText = "Testing $ip...") }
            logger.log("TEST", "=== TEST CONNECTION (no valid keys) ===")

            val config = SessionConfig(ByteArray(16), ByteArray(16), ByteArray(6), platformInfo = "Phone")
            val result = deps.sessionHandler.createSession(ip, config)

            result.onSuccess { logger.log("TEST", "Got 200 OK — unexpected with dummy keys!") }
            result.onFailure { e ->
                if (e.message?.contains("403") == true || e.message?.contains("Failed to parse") == true) {
                    logger.log("TEST", "Got response from PS3! Protocol works!")
                } else {
                    logger.error("TEST", "Connection failed: ${e.message}")
                }
            }
            updateState { copy(statusText = "Test done — check logs") }
        }
    }

    private fun register() {
        val s = _state.value
        val ip = cleanIp(s.ps3Ip)
        if (ip.isBlank()) { logger.error("REGIST", "PS3 IP is blank"); return }
        if (s.pin.length != 8) { logger.error("REGIST", "PIN must be exactly 8 digits"); return }

        val idBytes = s.deviceId.hexToByteArrayOrNull()
        val macBytes = s.deviceMac.hexToByteArrayOrNull()
        if (idBytes == null || idBytes.size != 16) { logger.error("REGIST", "Device ID must be 32 hex characters"); return }
        if (macBytes == null || macBytes.size != 6) { logger.error("REGIST", "MAC must be 12 hex characters"); return }

        logger.log("REGIST", "========== REGISTRATION START ==========")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateState { copy(statusText = "Registering with PIN ${s.pin}...") }

                val result = deps.registration.register(ip, s.pin, idBytes, macBytes, "PsOldRemotePlay", 1)
                result.onSuccess { res ->
                    val pkeyHex = res.pkey.toHex()
                    updateState { copy(pkey = pkeyHex, ps3Mac = res.ps3Mac, ps3Nickname = res.ps3Nickname, statusText = "REGISTERED! PKey obtained") }
                    logger.log("REGIST", "========== REGISTRATION COMPLETE ==========")
                    logger.log("REGIST", "PKey: $pkeyHex")
                }
                result.onFailure { e ->
                    logger.error("REGIST", "Phone registration FAILED: ${e.message}")
                    updateState { copy(statusText = "Trying PC type...") }

                    val result2 = deps.registration.register(ip, s.pin, idBytes, macBytes, "PsOldRemotePlay", 2)
                    result2.onSuccess { res ->
                        val pkeyHex = res.pkey.toHex()
                        updateState { copy(pkey = pkeyHex, statusText = "REGISTERED (as PC)!") }
                        logger.log("REGIST", "PKey: $pkeyHex")
                    }
                    result2.onFailure { e2 ->
                        logger.error("REGIST", "Both Phone and PC registration failed")
                        updateState { copy(statusText = "Registration failed: ${e2.message?.take(50)}") }
                    }
                }
            } catch (e: Exception) {
                logger.error("REGIST", "EXCEPTION: ${e.message}")
                updateState { copy(statusText = "Registration crashed: ${e.message?.take(50)}") }
            }
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            deps.sessionHandler.disconnect()
            deps.streaming.videoRenderer.stop()
            deps.streaming.audioRenderer.stop()
            deps.controllerInput.disconnect()
            updateState {
                copy(connectionStatus = ConnectionStatus.Disconnected, isStreaming = false,
                    videoPacketCount = 0, statusText = "Disconnected")
            }
            logger.log("SESSION", "Disconnected")
        }
    }

    private fun copyLogs() {
        val text = _state.value.logs.joinToString("\n") { entry ->
            val prefix = if (entry.isError) "[ERROR:${entry.tag}]" else "[${entry.tag}]"
            "[${entry.timestamp}]$prefix ${entry.message}"
        }
        viewModelScope.launch {
            _effects.send(Ps3Effect.CopyToClipboard(text))
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

    private inline fun updateState(transform: Ps3State.() -> Ps3State) {
        _state.update(transform)
    }

    companion object {
        fun cleanIp(raw: String): String {
            var ip = raw.trim()
            ip = ip.removePrefix("http://").removePrefix("https://")
            ip = ip.trimEnd('/')
            ip = ip.split(":").first()
            return ip
        }
    }
}

private fun currentTimestamp(): String {
    val now = Clock.System.now()
    val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d:%02d:%02d.%03d".format(
        local.hour, local.minute, local.second,
        now.toEpochMilliseconds() % 1000
    )
}

private fun String.hexToByteArrayOrNull(): ByteArray? {
    val clean = this.trim().replace(" ", "").replace(":", "").replace("-", "")
    if (clean.length % 2 != 0) return null
    return try {
        ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    } catch (_: Exception) { null }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }
