package com.my.psoldremoteplay

import com.my.psoldremoteplay.protocol.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class JvmPremoSession(private val crypto: PremoCrypto, private val logger: PremoLogger) : PremoSessionHandler {
    private var socket: Socket? = null

    override suspend fun createSession(ps3Ip: String, config: SessionConfig): Result<SessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val sock = Socket(ps3Ip, PremoConstants.PORT)
                val out = sock.getOutputStream()
                val inp = sock.getInputStream()

                val request = buildSessionRequest(config, crypto)
                logger.log("SESSION", "Sending session request to $ps3Ip:${PremoConstants.PORT}")
                logger.log("SESSION", request)

                out.write(request.toByteArray(Charsets.US_ASCII))
                out.flush()

                val responseBytes = readHttpResponse(inp)
                val responseText = String(responseBytes, Charsets.US_ASCII)
                logger.log("SESSION", "Response:\n$responseText")

                sock.close()

                val parsed = parseSessionResponse(responseText, crypto)
                if (parsed != null) {
                    Result.success(parsed)
                } else {
                    Result.failure(Exception("Failed to parse response. Raw:\n$responseText"))
                }
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

            val request = buildString {
                append("GET /sce/premo/session/video HTTP/1.1\r\n")
                append("SessionID: $sessionId\r\n")
                append("PREMO-Auth: $authToken\r\n")
                append("\r\n")
            }

            logger.log("VIDEO", "Starting video stream...")
            sock.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
            sock.getOutputStream().flush()

            // Skip HTTP response headers
            skipHttpHeaders(sock.getInputStream())

            val inp = sock.getInputStream()
            val headerBuf = ByteArray(32)
            var frameCount = 0

            while (!sock.isClosed) {
                readExactly(inp, headerBuf, 32)

                val payloadLen = ((headerBuf[16].toInt() and 0xFF) shl 8) or
                        (headerBuf[17].toInt() and 0xFF)

                if (payloadLen <= 0 || payloadLen > 0x100000) {
                    logger.error("VIDEO", "Invalid payload length: $payloadLen")
                    continue
                }

                val payload = ByteArray(payloadLen)
                readExactly(inp, payload, payloadLen)

                frameCount++
                if (frameCount % 30 == 0) {
                    logger.log("VIDEO", "Received $frameCount packets, last payload: $payloadLen bytes, magic: ${"%02X %02X".format(headerBuf[0], headerBuf[1])}")
                }

                onPacket(StreamPacket(
                    magic = headerBuf.copyOfRange(0, 2),
                    frame = ((headerBuf[2].toInt() and 0xFF) shl 8) or (headerBuf[3].toInt() and 0xFF),
                    clock = ((headerBuf[4].toLong() and 0xFF) shl 24) or
                            ((headerBuf[5].toLong() and 0xFF) shl 16) or
                            ((headerBuf[6].toLong() and 0xFF) shl 8) or
                            (headerBuf[7].toLong() and 0xFF),
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
    ) {
        // TODO: similar to video
    }

    override fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    private fun readHttpResponse(inp: InputStream): ByteArray {
        val buf = ByteArray(4096)
        val n = inp.read(buf)
        return if (n > 0) buf.copyOf(n) else ByteArray(0)
    }

    private fun skipHttpHeaders(inp: InputStream) {
        // Read until \r\n\r\n
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
