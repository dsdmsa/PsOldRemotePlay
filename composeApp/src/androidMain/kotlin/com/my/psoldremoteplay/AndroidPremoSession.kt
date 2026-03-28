package com.my.psoldremoteplay

import com.my.psoldremoteplay.protocol.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.Socket

class AndroidPremoSession(private val crypto: PremoCrypto, private val logger: PremoLogger) : PremoSessionHandler {
    private var socket: Socket? = null

    override suspend fun createSession(ps3Ip: String, config: SessionConfig): Result<SessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val sock = Socket(ps3Ip, PremoConstants.PORT)
                val request = buildSessionRequest(config, crypto)
                logger.log("SESSION", "Sending session request to $ps3Ip:${PremoConstants.PORT}")

                sock.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
                sock.getOutputStream().flush()

                val responseBytes = ByteArray(4096)
                val n = sock.getInputStream().read(responseBytes)
                val responseText = if (n > 0) String(responseBytes, 0, n, Charsets.US_ASCII) else ""
                logger.log("SESSION", "Response:\n$responseText")
                sock.close()

                val parsed = parseSessionResponse(responseText, crypto)
                if (parsed != null) Result.success(parsed)
                else Result.failure(Exception("Failed to parse response. Raw:\n$responseText"))
            } catch (e: Exception) {
                logger.error("SESSION", "Connection failed: ${e.message}", e)
                Result.failure(e)
            }
        }

    override suspend fun startVideoStream(
        ps3Ip: String, sessionId: String, authToken: String, onPacket: (StreamPacket) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val sock = Socket(ps3Ip, PremoConstants.PORT)
            socket = sock
            sock.soTimeout = 0

            val request = "GET /sce/premo/session/video HTTP/1.1\r\nSessionID: $sessionId\r\nPREMO-Auth: $authToken\r\n\r\n"
            sock.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
            sock.getOutputStream().flush()

            skipHttpHeaders(sock.getInputStream())

            val inp = sock.getInputStream()
            val headerBuf = ByteArray(32)
            var frameCount = 0

            while (!sock.isClosed) {
                readExactly(inp, headerBuf, 32)
                val payloadLen = ((headerBuf[16].toInt() and 0xFF) shl 8) or (headerBuf[17].toInt() and 0xFF)
                if (payloadLen <= 0 || payloadLen > 0x100000) continue

                val payload = ByteArray(payloadLen)
                readExactly(inp, payload, payloadLen)
                frameCount++

                if (frameCount % 30 == 0) {
                    logger.log("VIDEO", "Received $frameCount packets, last: $payloadLen bytes")
                }

                onPacket(StreamPacket(
                    magic = headerBuf.copyOfRange(0, 2),
                    frame = ((headerBuf[2].toInt() and 0xFF) shl 8) or (headerBuf[3].toInt() and 0xFF),
                    clock = ((headerBuf[4].toLong() and 0xFF) shl 24) or ((headerBuf[5].toLong() and 0xFF) shl 16) or
                            ((headerBuf[6].toLong() and 0xFF) shl 8) or (headerBuf[7].toLong() and 0xFF),
                    payloadLength = payloadLen,
                    rawHeader = headerBuf.copyOf(),
                    payload = payload
                ))
            }
        } catch (e: Exception) {
            logger.error("VIDEO", "Video stream error: ${e.message}", e)
        }
    }

    override suspend fun startAudioStream(
        ps3Ip: String, sessionId: String, authToken: String, onPacket: (StreamPacket) -> Unit
    ) { /* TODO */ }

    override fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    private fun skipHttpHeaders(inp: InputStream) {
        var crlfCount = 0
        while (crlfCount < 4) {
            val b = inp.read()
            if (b == -1) break
            if (b == 0x0D || b == 0x0A) crlfCount++ else crlfCount = 0
        }
    }

    private fun readExactly(inp: InputStream, buf: ByteArray, len: Int) {
        var offset = 0
        while (offset < len) {
            val n = inp.read(buf, offset, len - offset)
            if (n == -1) throw java.io.EOFException("Stream ended")
            offset += n
        }
    }
}
