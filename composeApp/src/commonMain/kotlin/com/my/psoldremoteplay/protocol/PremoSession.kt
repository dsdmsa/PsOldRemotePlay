package com.my.psoldremoteplay.protocol

data class SessionConfig(
    val pkey: ByteArray,
    val deviceId: ByteArray,   // 16 bytes — PSPID
    val deviceMac: ByteArray,  // 6 bytes
    val deviceName: String = "PsOldRemotePlay",
    val platformInfo: String = "Phone"
)

data class SessionResponse(
    val sessionId: String,
    val nonce: ByteArray,
    val videoCodec: String,
    val videoResolution: String,
    val videoFramerate: Int,
    val videoBitrate: Int,
    val audioCodec: String,
    val execMode: String,
    val ps3Nickname: String,
    val rawHeaders: Map<String, String>
)

interface PremoSessionHandler {
    suspend fun createSession(ps3Ip: String, config: SessionConfig): Result<SessionResponse>
    suspend fun startVideoStream(
        ps3Ip: String,
        sessionId: String,
        authToken: String,
        aesKey: ByteArray,    // for packet decryption
        aesIv: ByteArray,     // for packet decryption (fresh copy per packet)
        onPacket: suspend (StreamPacket) -> Unit
    )
    suspend fun startAudioStream(
        ps3Ip: String,
        sessionId: String,
        authToken: String,
        aesKey: ByteArray,
        aesIv: ByteArray,
        onPacket: suspend (StreamPacket) -> Unit
    )
    fun disconnect()
}

data class StreamPacket(
    val magic: ByteArray,      // 2 bytes — stream type
    val frame: Int,            // frame counter
    val clock: Long,           // timestamp
    val payloadLength: Int,
    val rawHeader: ByteArray,  // full 32-byte header
    val payload: ByteArray     // decrypted payload
)

fun buildSessionRequest(config: SessionConfig, crypto: PremoCrypto): String {
    val pspId = crypto.base64Encode(config.deviceId)
    val userName = crypto.base64Encode(config.deviceName.toByteArray())
    return buildString {
        append("GET /sce/premo/session HTTP/1.1\r\n")
        append("PREMO-PSPID: $pspId\r\n")
        append("PREMO-Version: ${PremoConstants.PREMO_VERSION_MAJOR}.${PremoConstants.PREMO_VERSION_MINOR}\r\n")
        append("PREMO-Mode: PREMO\r\n")
        append("PREMO-Platform-Info: ${config.platformInfo}\r\n")
        append("PREMO-UserName: $userName\r\n")
        append("PREMO-Trans: capable\r\n")
        append("PREMO-Video-Codec-Ability: M4V,AVC,AVC/CAVLC\r\n")
        append("PREMO-Video-Resolution: ${PremoConstants.FRAME_WIDTH}x${PremoConstants.FRAME_HEIGHT}\r\n")
        append("PREMO-Video-Bitrate: 512000\r\n")
        append("PREMO-Video-Framerate: 30\r\n")
        append("PREMO-Audio-Codec-Ability: M4A,ATRAC\r\n")
        append("PREMO-Audio-Bitrate: 128000\r\n")
        append("\r\n")
    }
}

fun parseSessionResponse(responseText: String, crypto: PremoCrypto? = null): SessionResponse? {
    val headers = mutableMapOf<String, String>()
    val lines = responseText.split("\r\n")
    if (lines.isEmpty() || !lines[0].contains("200 OK")) return null

    for (line in lines.drop(1)) {
        val sep = line.indexOf(':')
        if (sep > 0) {
            headers[line.substring(0, sep).trim()] = line.substring(sep + 1).trim()
        }
    }

    val nonce = headers["PREMO-Nonce"]?.let { base64 ->
        try { crypto?.base64Decode(base64) } catch (_: Exception) { null }
    } ?: ByteArray(16)

    return SessionResponse(
        sessionId = headers["SessionID"] ?: "",
        nonce = nonce,
        videoCodec = headers["PREMO-Video-Codec"] ?: "AVC",
        videoResolution = headers["PREMO-Video-Resolution"] ?: "480x272",
        videoFramerate = headers["PREMO-Video-Framerate"]?.toIntOrNull() ?: 30,
        videoBitrate = headers["PREMO-Video-Bitrate"]?.toIntOrNull() ?: 512000,
        audioCodec = headers["PREMO-Audio-Codec"] ?: "M4A",
        execMode = headers["PREMO-Exec-Mode"] ?: "VSH",
        ps3Nickname = headers["PREMO-PS3-Nickname"] ?: "",
        rawHeaders = headers
    )
}
