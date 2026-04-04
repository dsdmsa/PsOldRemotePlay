package com.my.psremoteplay.feature.ps3.platform

import com.my.psremoteplay.core.streaming.Crypto
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps3.protocol.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class JvmPremoSession(private val crypto: Crypto, private val logger: Logger) : PremoSessionHandler {
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
        ps3Ip: String,
        sessionId: String,
        authToken: String,
        aesKey: ByteArray,
        aesIv: ByteArray,
        onPacket: suspend (StreamPacket) -> Unit
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

            // Read HTTP chunked transfer encoding
            while (!sock.isClosed) {
                // Read chunk size line (hex digits followed by \r\n)
                val chunkSizeLine = readLine(inp) ?: break
                val chunkSize = try {
                    chunkSizeLine.trim().toInt(16)
                } catch (e: NumberFormatException) {
                    logger.error("VIDEO", "Invalid chunk size: '$chunkSizeLine'")
                    break
                }

                if (chunkSize == 0) {
                    // End of chunked stream
                    logger.log("VIDEO", "Received final chunk (size 0), stream complete")
                    break
                }

                // Read the chunk data (32-byte header + payload)
                val chunkData = ByteArray(chunkSize)
                readExactly(inp, chunkData, chunkSize)

                // Read trailing \r\n after chunk
                val trailingCrlf = ByteArray(2)
                readExactly(inp, trailingCrlf, 2)
                if (trailingCrlf[0] != 0x0D.toByte() || trailingCrlf[1] != 0x0A.toByte()) {
                    logger.error("VIDEO", "Missing CRLF after chunk")
                }

                // Parse chunk data: first 32 bytes = header, rest = payload
                if (chunkSize < 32) {
                    logger.error("VIDEO", "Chunk too small: $chunkSize bytes (need at least 32)")
                    continue
                }

                chunkData.copyInto(headerBuf, 0, 0, 32)
                var payload = chunkData.copyOfRange(32, chunkSize)

                // Check if payload is encrypted: magic 0xFF/0xFE (H.264) && unk6==0x0401
                val isH264 = (headerBuf[1].toInt() and 0xFF) == 0xFF || (headerBuf[1].toInt() and 0xFF) == 0xFE
                val unk6 = ((headerBuf[22].toInt() and 0xFF) shl 8) or (headerBuf[23].toInt() and 0xFF)
                val isEncrypted = isH264 && unk6 == 0x0401

                // Decrypt if needed (H.264 keyframes with unk6==0x0401)
                if (isEncrypted && payload.isNotEmpty()) {
                    val freshIv = aesIv.copyOf()
                    val decryptLen = (payload.size / 16) * 16  // only full blocks
                    if (decryptLen > 0) {
                        val decrypted = crypto.aesDecrypt(payload.copyOfRange(0, decryptLen), aesKey, freshIv)
                        payload = decrypted + payload.copyOfRange(decryptLen, payload.size)
                    }
                }

                frameCount++
                if (frameCount % 30 == 0) {
                    logger.log("VIDEO", "Received $frameCount packets, last payload: ${payload.size} bytes, magic: ${"%02X %02X".format(headerBuf[0], headerBuf[1])}")
                }

                onPacket(StreamPacket(
                    magic = headerBuf.copyOfRange(0, 2),
                    frame = ((headerBuf[2].toInt() and 0xFF) shl 8) or (headerBuf[3].toInt() and 0xFF),
                    clock = ((headerBuf[4].toLong() and 0xFF) shl 24) or
                            ((headerBuf[5].toLong() and 0xFF) shl 16) or
                            ((headerBuf[6].toLong() and 0xFF) shl 8) or
                            (headerBuf[7].toLong() and 0xFF),
                    payloadLength = payload.size,
                    rawHeader = headerBuf.copyOf(),
                    payload = payload
                ))
            }
        } catch (e: Exception) {
            logger.error("VIDEO", "Video stream error: ${e.message}", e)
        }
    }

    override suspend fun startAudioStream(
        ps3Ip: String,
        sessionId: String,
        authToken: String,
        aesKey: ByteArray,
        aesIv: ByteArray,
        onPacket: suspend (StreamPacket) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val sock = Socket(ps3Ip, PremoConstants.PORT)
            socket = sock
            sock.soTimeout = 0

            val request = buildString {
                append("GET /sce/premo/session/audio HTTP/1.1\r\n")
                append("SessionID: $sessionId\r\n")
                append("PREMO-Auth: $authToken\r\n")
                append("\r\n")
            }

            logger.log("AUDIO", "Starting audio stream...")
            sock.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
            sock.getOutputStream().flush()

            skipHttpHeaders(sock.getInputStream())

            val inp = sock.getInputStream()
            val headerBuf = ByteArray(32)
            var frameCount = 0

            while (!sock.isClosed) {
                val chunkSizeLine = readLine(inp) ?: break
                val chunkSize = try {
                    chunkSizeLine.trim().toInt(16)
                } catch (e: NumberFormatException) {
                    break
                }

                if (chunkSize == 0) break

                val chunkData = ByteArray(chunkSize)
                readExactly(inp, chunkData, chunkSize)
                val trailingCrlf = ByteArray(2)
                readExactly(inp, trailingCrlf, 2)

                if (chunkSize < 32) continue

                chunkData.copyInto(headerBuf, 0, 0, 32)
                var payload = chunkData.copyOfRange(32, chunkSize)

                // Audio encryption (magic 0x80 = AAC)
                val isAudio = (headerBuf[1].toInt() and 0xFF) == 0x80
                val isEncrypted = isAudio

                if (isEncrypted && payload.isNotEmpty()) {
                    val freshIv = aesIv.copyOf()
                    val decryptLen = (payload.size / 16) * 16
                    if (decryptLen > 0) {
                        val decrypted = crypto.aesDecrypt(payload.copyOfRange(0, decryptLen), aesKey, freshIv)
                        payload = decrypted + payload.copyOfRange(decryptLen, payload.size)
                    }
                }

                frameCount++
                if (frameCount % 10 == 0) {
                    logger.log("AUDIO", "Received $frameCount packets")
                }

                onPacket(StreamPacket(
                    magic = headerBuf.copyOfRange(0, 2),
                    frame = ((headerBuf[2].toInt() and 0xFF) shl 8) or (headerBuf[3].toInt() and 0xFF),
                    clock = ((headerBuf[4].toLong() and 0xFF) shl 24) or
                            ((headerBuf[5].toLong() and 0xFF) shl 16) or
                            ((headerBuf[6].toLong() and 0xFF) shl 8) or
                            (headerBuf[7].toLong() and 0xFF),
                    payloadLength = payload.size,
                    rawHeader = headerBuf.copyOf(),
                    payload = payload
                ))
            }
        } catch (e: Exception) {
            logger.error("AUDIO", "Audio stream error: ${e.message}", e)
        }
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

    private fun readLine(inp: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = inp.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == 0x0A) { // \n
                if (sb.isNotEmpty() && sb.last() == '\r') {
                    return sb.dropLast(1).toString()
                }
                return sb.toString()
            }
            sb.append(b.toChar())
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
