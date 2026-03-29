package com.my.psoldremoteplay.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.psoldremoteplay.di.PlatformDependencies
import com.my.psoldremoteplay.protocol.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RemotePlayViewModel(private val deps: PlatformDependencies) : ViewModel() {

    private val _state = MutableStateFlow(RemotePlayState())
    val state: StateFlow<RemotePlayState> = _state.asStateFlow()

    private val _effects = Channel<RemotePlayEffect>(Channel.BUFFERED)
    val effects: Flow<RemotePlayEffect> = _effects.receiveAsFlow()

    private val logger = object : PremoLogger {
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

    fun onIntent(intent: RemotePlayIntent) {
        when (intent) {
            is RemotePlayIntent.UpdateIp -> updateState { copy(ps3Ip = cleanIp(intent.ip)) }
            is RemotePlayIntent.UpdatePkey -> updateState { copy(pkey = intent.pkey) }
            is RemotePlayIntent.UpdatePin -> updateState { copy(pin = intent.pin.filter { it.isDigit() }.take(8)) }
            is RemotePlayIntent.UpdateDeviceId -> updateState { copy(deviceId = intent.id) }
            is RemotePlayIntent.UpdateDeviceMac -> updateState { copy(deviceMac = intent.mac) }
            is RemotePlayIntent.DiscoverBroadcast -> discoverBroadcast()
            is RemotePlayIntent.DiscoverDirect -> discoverDirect(intent.ip)
            is RemotePlayIntent.ConnectSession -> connectSession()
            is RemotePlayIntent.TestConnection -> testConnection()
            is RemotePlayIntent.Register -> register()
            is RemotePlayIntent.Disconnect -> disconnect()
            is RemotePlayIntent.ClearLogs -> updateState { copy(logs = emptyList()) }
            is RemotePlayIntent.CopyLogs -> copyLogs()
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
                        ps3Ip = info.ip,
                        ps3Nickname = info.nickname,
                        ps3Mac = info.macString,
                        connectionStatus = ConnectionStatus.Disconnected,
                        statusText = "Found: ${info.ip}"
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
        if (cleanedIp.isBlank()) {
            logger.error("DISCOVERY", "Enter PS3 IP first")
            return
        }
        viewModelScope.launch {
            updateState { copy(ps3Ip = cleanedIp, connectionStatus = ConnectionStatus.Discovering, statusText = "Direct discovery to $cleanedIp...") }
            logger.log("DISCOVERY", "Sending SRCH directly to $cleanedIp...")

            val info = deps.discoverer.discoverDirect(cleanedIp, timeoutMs = 3000)
            if (info != null) {
                updateState {
                    copy(
                        ps3Nickname = info.nickname,
                        ps3Mac = info.macString,
                        connectionStatus = ConnectionStatus.Disconnected,
                        statusText = "Found: ${info.nickname}"
                    )
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

        if (pkeyBytes == null || pkeyBytes.size != 16) {
            logger.error("SESSION", "PKey must be 32 hex chars (16 bytes)")
            return
        }
        if (idBytes == null || idBytes.size != 16) {
            logger.error("SESSION", "Device ID must be 32 hex chars")
            return
        }
        if (macBytes == null || macBytes.size != 6) {
            logger.error("SESSION", "MAC must be 12 hex chars")
            return
        }

        viewModelScope.launch {
            updateState { copy(connectionStatus = ConnectionStatus.Connecting, statusText = "Connecting to $ip...") }
            logger.log("SESSION", "Connecting to $ip:${PremoConstants.PORT}...")
            logger.log("SESSION", "PKey: ${pkeyBytes.toHex()}")
            logger.log("SESSION", "DeviceID: ${idBytes.toHex()}")
            logger.log("SESSION", "DeviceMAC: ${macBytes.toHex()}")

            val config = SessionConfig(pkeyBytes, idBytes, macBytes)
            val result = deps.sessionHandler.createSession(ip, config)

            result.onSuccess { resp ->
                updateState {
                    copy(
                        sessionId = resp.sessionId,
                        connectionStatus = ConnectionStatus.Connected,
                        statusText = "Connected! ${resp.execMode} | ${resp.videoCodec} ${resp.videoResolution}"
                    )
                }
                logger.log("SESSION", "=== SESSION ESTABLISHED ===")
                logger.log("SESSION", "SessionID: ${resp.sessionId}")
                logger.log("SESSION", "ExecMode: ${resp.execMode}")
                logger.log("SESSION", "Video: ${resp.videoCodec} ${resp.videoResolution} @ ${resp.videoFramerate}fps ${resp.videoBitrate}bps")
                logger.log("SESSION", "Audio: ${resp.audioCodec}")
                logger.log("SESSION", "PS3 Nickname: ${resp.ps3Nickname}")
                logger.log("SESSION", "Nonce: ${resp.nonce.toHex()}")
                resp.rawHeaders.forEach { (k, v) -> logger.log("SESSION", "  $k: $v") }

                val authToken = generateAuthToken(deps.crypto, pkeyBytes, resp.nonce, macBytes)
                logger.log("SESSION", "Auth token: $authToken")

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

            // Compute session encryption keys
            val aesKey = xorBytes(pkeyBytes, PremoConstants.SKEY0)
            val aesIv = xorBytes(nonce, PremoConstants.SKEY2)

            deps.videoRenderer.start()
            logger.log("VIDEO", "Starting video stream...")
            deps.sessionHandler.startVideoStream(ip, sessionId, authToken, aesKey, aesIv) { packet ->
                deps.videoRenderer.onStreamPacket(packet.rawHeader, packet.payload, false)
                updateState { copy(videoPacketCount = videoPacketCount + 1) }
            }

            // Start audio in parallel
            deps.audioRenderer.start()
            logger.log("AUDIO", "Starting audio stream...")
            deps.sessionHandler.startAudioStream(ip, sessionId, authToken, aesKey, aesIv) { packet ->
                logger.log("AUDIO", "Audio packet: ${packet.payload.size} bytes, magic: ${"%02X".format(packet.magic[1])}")
                // Audio rendering will be handled by AudioRenderer when decoders are implemented
            }
        }
    }

    private fun testConnection() {
        val ip = cleanIp(_state.value.ps3Ip)
        if (ip.isBlank()) { logger.error("TEST", "Set PS3 IP first"); return }

        viewModelScope.launch {
            updateState { copy(statusText = "Testing $ip...") }
            logger.log("TEST", "=== TEST CONNECTION (no valid keys) ===")
            logger.log("TEST", "Connecting to $ip:${PremoConstants.PORT}...")
            logger.log("TEST", "Sending dummy session request — expecting 403 Forbidden...")

            val config = SessionConfig(ByteArray(16), ByteArray(16), ByteArray(6), platformInfo = "Phone")
            val result = deps.sessionHandler.createSession(ip, config)

            result.onSuccess {
                logger.log("TEST", "Got 200 OK — unexpected with dummy keys!")
            }
            result.onFailure { e ->
                if (e.message?.contains("403") == true || e.message?.contains("Failed to parse") == true) {
                    logger.log("TEST", "Got response from PS3! (403 = keys rejected but PS3 is listening)")
                    logger.log("TEST", "Protocol works! PS3 is reachable on port ${PremoConstants.PORT}")
                } else {
                    logger.error("TEST", "Connection failed: ${e.message}")
                    logger.log("TEST", "Check: is PS3 on? Is Remote Play enabled? Same network?")
                }
            }
            updateState { copy(statusText = "Test done — check logs") }
        }
    }

    private fun register() {
        val s = _state.value
        val ip = cleanIp(s.ps3Ip)
        if (ip.isBlank()) { logger.error("REGIST", "Set PS3 IP first"); return }
        if (s.pin.length != 8) { logger.error("REGIST", "PIN must be 8 digits (shown on PS3 screen)"); return }

        val idBytes = s.deviceId.hexToByteArrayOrNull()
        val macBytes = s.deviceMac.hexToByteArrayOrNull()
        if (idBytes == null || idBytes.size != 16) { logger.error("REGIST", "Device ID must be 32 hex chars"); return }
        if (macBytes == null || macBytes.size != 6) { logger.error("REGIST", "MAC must be 12 hex chars"); return }

        viewModelScope.launch {
            updateState { copy(statusText = "Registering with PIN ${s.pin}...") }
            logger.log("REGIST", "Attempting registration as Phone (type 1)...")

            val result = deps.registration.register(ip, s.pin, idBytes, macBytes, "PsOldRemotePlay", 1)
            result.onSuccess { res ->
                val pkeyHex = res.pkey.toHex()
                updateState { copy(pkey = pkeyHex, ps3Mac = res.ps3Mac, ps3Nickname = res.ps3Nickname, statusText = "REGISTERED! PKey obtained") }
                logger.log("REGIST", "=== SAVE THESE VALUES ===")
                logger.log("REGIST", "PKey: $pkeyHex")
                logger.log("REGIST", "PS3 MAC: ${res.ps3Mac}")
                logger.log("REGIST", "PS3 Nickname: ${res.ps3Nickname}")
            }
            result.onFailure { e ->
                updateState { copy(statusText = "Registration failed: ${e.message?.take(50)}") }
                logger.log("REGIST", "Phone registration failed, trying PC (type 2)...")
                val result2 = deps.registration.register(ip, s.pin, idBytes, macBytes, "PsOldRemotePlay", 2)
                result2.onSuccess { res ->
                    val pkeyHex = res.pkey.toHex()
                    updateState { copy(pkey = pkeyHex, statusText = "REGISTERED (as PC)! PKey obtained") }
                    logger.log("REGIST", "=== PKey: $pkeyHex ===")
                }
                result2.onFailure {
                    logger.error("REGIST", "Both Phone and PC registration failed")
                }
            }
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            deps.sessionHandler.disconnect()
            deps.videoRenderer.stop()
            deps.audioRenderer.stop()
            deps.controllerInput.disconnect()
            updateState {
                copy(
                    connectionStatus = ConnectionStatus.Disconnected,
                    isStreaming = false,
                    videoPacketCount = 0,
                    statusText = "Disconnected"
                )
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
            _effects.send(RemotePlayEffect.CopyToClipboard(text))
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

    private inline fun updateState(transform: RemotePlayState.() -> RemotePlayState) {
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
    // Simple cross-platform timestamp — platform can override via logger
    val millis = kotlinx.datetime.Clock.System.now()
    return millis.toString().substringAfter("T").substringBefore("Z").take(12)
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
