package com.my.psoldremoteplay

import com.my.psoldremoteplay.protocol.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.Socket

class JvmPremoRegistration(private val crypto: PremoCrypto, private val logger: PremoLogger) : PremoRegistration {

    override suspend fun register(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        platformType: Int
    ): Result<com.my.psoldremoteplay.protocol.RegistrationResult> = withContext(Dispatchers.IO) {
        try {
            logger.log("REGIST", "=== STARTING REGISTRATION ===")
            logger.log("REGIST", "PS3: $ps3Ip, PIN: $pin, Platform: ${platformName(platformType)}")
            logger.log("REGIST", "DeviceID: ${deviceId.joinToString("") { "%02X".format(it) }}")
            logger.log("REGIST", "DeviceMac: ${deviceMac.joinToString("") { "%02X".format(it) }}")

            // Build plaintext registration body
            val bodyText = buildString {
                append("Client-Type: ${clientTypeHeader(platformType)}\r\n")
                append("Client-Id: ${deviceId.joinToString("") { "%02x".format(it) }}\r\n")
                append("Client-Mac: ${deviceMac.joinToString("") { "%02x".format(it) }}\r\n")
                append("Client-Nickname: $deviceName\r\n")
            }
            logger.log("REGIST", "Plaintext body:\n$bodyText")

            // Generate 16 random bytes as key material
            val keyMaterial = crypto.randomBytes(16)
            logger.log("REGIST", "Key material: ${keyMaterial.joinToString("") { "%02X".format(it) }}")

            // Derive AES key based on platform type
            val aesKey = deriveRegistrationKey(keyMaterial, platformType)
            logger.log("REGIST", "Derived AES key: ${aesKey.joinToString("") { "%02X".format(it) }}")

            // Derive IV
            val aesIv = deriveRegistrationIv(platformType, pin)
            logger.log("REGIST", "Derived AES IV: ${aesIv.joinToString("") { "%02X".format(it) }}")

            // Pad plaintext to 16-byte boundary
            val plainBytes = bodyText.toByteArray(Charsets.US_ASCII)
            val paddedLen = (plainBytes.size + 15) and 0x7FFFFFF0
            val padded = ByteArray(paddedLen)
            plainBytes.copyInto(padded)
            logger.log("REGIST", "Plaintext: ${plainBytes.size} bytes, padded to $paddedLen")

            // Encrypt
            val encrypted = crypto.aesEncrypt(padded, aesKey, aesIv)
            logger.log("REGIST", "Encrypted body: ${encrypted.size} bytes")

            // Build full body: encrypted data + 16 bytes key material
            val fullBody = encrypted + keyMaterial
            logger.log("REGIST", "Full body: ${fullBody.size} bytes (encrypted: ${encrypted.size} + key: 16)")

            // Send HTTP POST
            val socket = Socket(ps3Ip, PremoConstants.PORT)
            socket.soTimeout = 10000
            val out = socket.getOutputStream()

            val httpRequest = "POST /sce/premo/regist HTTP/1.1\r\nContent-Length: ${fullBody.size}\r\n\r\n"
            logger.log("REGIST", "Sending: $httpRequest")

            out.write(httpRequest.toByteArray(Charsets.US_ASCII))
            out.write(fullBody)
            out.flush()
            logger.log("REGIST", "Request sent, waiting for response...")

            // Read response
            val response = readFullResponse(socket.inputStream)
            val responseText = String(response, Charsets.US_ASCII)
            socket.close()

            logger.log("REGIST", "Raw response (${response.size} bytes):")
            logger.log("REGIST", responseText.take(500))
            logger.log("REGIST", "Response hex (first 128 bytes): ${response.take(128).joinToString(" ") { "%02X".format(it) }}")

            if (responseText.contains("200 OK")) {
                logger.log("REGIST", "=== GOT 200 OK! Attempting to decrypt response ===")
                val result = parseRegistrationResponse(response, aesKey, aesIv)
                if (result != null) {
                    logger.log("REGIST", "=== REGISTRATION SUCCESSFUL ===")
                    logger.log("REGIST", "PKey: ${result.pkey.joinToString("") { "%02X".format(it) }}")
                    logger.log("REGIST", "PS3 MAC: ${result.ps3Mac}")
                    logger.log("REGIST", "PS3 Nickname: ${result.ps3Nickname}")
                    Result.success(result)
                } else {
                    logger.log("REGIST", "Got 200 OK but failed to parse/decrypt response body")
                    Result.failure(Exception("Failed to decrypt registration response"))
                }
            } else if (responseText.contains("403")) {
                logger.error("REGIST", "Got 403 Forbidden — registration rejected")
                logger.log("REGIST", "This could mean: wrong encryption, wrong PIN, or wrong platform type")
                logger.log("REGIST", "Full response:\n$responseText")
                Result.failure(Exception("403 Forbidden — registration rejected"))
            } else {
                logger.error("REGIST", "Unexpected response: ${responseText.take(200)}")
                Result.failure(Exception("Unexpected response"))
            }
        } catch (e: Exception) {
            logger.error("REGIST", "Registration failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Client-Type header value for registration.
     * IMPORTANT: The PS3 uses a SHIFTED mapping (confirmed from decompiled FUN_00100168):
     *   PS3 menu "PSP"    (type 0, PSP encryption)  → expects Client-Type: "Phone"
     *   PS3 menu "Phone"  (type 1, Phone encryption) → expects Client-Type: "PC"
     *   PS3 menu "PC"     (type 2, PC encryption)    → expects Client-Type: "VITA"
     */
    private fun clientTypeHeader(type: Int) = when (type) {
        0 -> "Phone"   // PSP encryption → Client-Type "Phone"
        1 -> "PC"      // Phone encryption → Client-Type "PC"
        2 -> "VITA"    // PC encryption → Client-Type "VITA"
        3 -> "VITA"
        else -> "PC"
    }

    private fun platformName(type: Int) = when (type) {
        0 -> "PSP"
        1 -> "Phone"
        2 -> "PC"
        3 -> "VITA"
        else -> "Phone"
    }

    private fun deriveRegistrationKey(keyMaterial: ByteArray, platformType: Int): ByteArray {
        val key = ByteArray(16)
        when (platformType) {
            0 -> { // PSP: key[i] = (material[i] XOR static[i]) - i - 0x25
                for (i in 0..15) {
                    key[i] = ((keyMaterial[i].toInt() and 0xFF xor (PremoConstants.REG_XOR_PSP[i].toInt() and 0xFF)) - i - 0x25).toByte()
                }
            }
            1 -> { // Phone: key[i] = (material[i] - i - 0x28) XOR static[i]
                for (i in 0..15) {
                    key[i] = (((keyMaterial[i].toInt() and 0xFF) - i - 0x28) xor (PremoConstants.REG_XOR_PHONE[i].toInt() and 0xFF)).toByte()
                }
            }
            else -> { // PC/VITA: key[i] = (material[i] XOR static[i]) - i - 0x2B
                for (i in 0..15) {
                    key[i] = ((keyMaterial[i].toInt() and 0xFF xor (PremoConstants.REG_XOR_PC[i].toInt() and 0xFF)) - i - 0x2B).toByte()
                }
            }
        }
        return key
    }

    private fun deriveRegistrationIv(platformType: Int, pin: String): ByteArray {
        val baseIv = when (platformType) {
            0 -> PremoConstants.REG_IV_PSP.copyOf()
            1 -> PremoConstants.REG_IV_PHONE.copyOf()
            else -> PremoConstants.REG_IV_PC.copyOf()
        }

        // The IV is XOR'd with an 8-byte context value
        // We'll try: PIN as 8 bytes, zeros, and other patterns
        // For now try the PIN converted to bytes
        val contextBytes = pinToContextBytes(pin)
        logger.log("REGIST", "IV context (from PIN): ${contextBytes.joinToString("") { "%02X".format(it) }}")

        when (platformType) {
            1 -> { // Phone: XOR second 8 bytes of IV
                for (i in 0..7) {
                    baseIv[8 + i] = (baseIv[8 + i].toInt() xor contextBytes[i].toInt()).toByte()
                }
            }
            else -> { // PSP/PC: XOR first 8 bytes of IV
                for (i in 0..7) {
                    baseIv[i] = (baseIv[i].toInt() xor contextBytes[i].toInt()).toByte()
                }
            }
        }
        return baseIv
    }

    /**
     * Convert PIN to 8-byte IV context value.
     *
     * From decompiled FUN_00100168 + FUN_000fe6b8 call chain:
     *   *param_2 = (longlong)(int)PIN  → stored as big-endian 64-bit int
     *   uStack_80/uStack_78 ^= **(ulonglong **)(param_1 + 0x38)
     *
     * PS4 comparison (kingcreek/ps4-remote-play) uses:
     *   struct.pack('!L', pin) + b'\x00' * 12  → 4-byte BE uint
     *
     * We try PIN as big-endian longlong (confirmed from code tracing).
     */
    private fun pinToContextBytes(pin: String): ByteArray {
        val pinInt = pin.toLongOrNull() ?: 0L
        val bytes = ByteArray(8)
        for (i in 7 downTo 0) {
            bytes[i] = (pinInt shr ((7 - i) * 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun parseRegistrationResponse(
        rawResponse: ByteArray,
        aesKey: ByteArray,
        aesIv: ByteArray
    ): RegistrationResult? {
        // Find the HTTP body (after \r\n\r\n)
        val headerEnd = findHeaderEnd(rawResponse) ?: return null
        val headers = String(rawResponse, 0, headerEnd, Charsets.US_ASCII)
        logger.log("REGIST", "Response headers:\n$headers")

        // Extract Content-Length
        val contentLength = Regex("Content-Length:\\s*(\\d+)").find(headers)?.groupValues?.get(1)?.toIntOrNull()
        logger.log("REGIST", "Content-Length: $contentLength")

        val bodyStart = headerEnd + 4 // skip \r\n\r\n
        val body = rawResponse.copyOfRange(bodyStart, rawResponse.size)
        logger.log("REGIST", "Response body: ${body.size} bytes")
        logger.log("REGIST", "Body hex: ${body.joinToString(" ") { "%02X".format(it) }}")

        if (body.isEmpty()) {
            logger.log("REGIST", "Empty response body")
            return null
        }

        // Try to decrypt
        return try {
            val decrypted = crypto.aesDecrypt(body, aesKey, aesIv)
            val decryptedText = String(decrypted, Charsets.US_ASCII)
            logger.log("REGIST", "Decrypted response:\n$decryptedText")

            // Parse key-value pairs
            val fields = mutableMapOf<String, String>()
            for (line in decryptedText.split("\r\n", "\n")) {
                val sep = line.indexOf(':')
                if (sep > 0) {
                    fields[line.substring(0, sep).trim()] = line.substring(sep + 1).trim()
                }
            }

            val pkeyHex = fields["PREMO-Key"] ?: ""
            val pkeyBytes = pkeyHex.trim().hexToByteArraySafe()

            com.my.psoldremoteplay.protocol.RegistrationResult(
                pkey = pkeyBytes,
                ps3Mac = fields["PS3-Mac"] ?: "",
                ps3Nickname = fields["PS3-Nickname"] ?: "",
                deviceId = ByteArray(16),
                deviceMac = ByteArray(6)
            )
        } catch (e: Exception) {
            logger.error("REGIST", "Decryption failed: ${e.message}")
            logger.log("REGIST", "Trying with unencrypted body...")
            // Maybe the response isn't encrypted?
            val bodyText = String(body, Charsets.US_ASCII)
            logger.log("REGIST", "Raw body as text: $bodyText")
            null
        }
    }

    private fun findHeaderEnd(data: ByteArray): Int? {
        for (i in 0 until data.size - 3) {
            if (data[i] == 0x0D.toByte() && data[i + 1] == 0x0A.toByte() &&
                data[i + 2] == 0x0D.toByte() && data[i + 3] == 0x0A.toByte()) {
                return i
            }
        }
        return null
    }

    private fun readFullResponse(inp: InputStream): ByteArray {
        val buf = ByteArray(8192)
        var total = 0
        val allData = mutableListOf<Byte>()
        try {
            while (true) {
                val n = inp.read(buf)
                if (n <= 0) break
                for (i in 0 until n) allData.add(buf[i])
                total += n
                // Check if we have complete response (Content-Length: 0 or body received)
                val current = allData.toByteArray()
                val text = String(current, Charsets.US_ASCII)
                if (text.contains("\r\n\r\n")) {
                    val cl = Regex("Content-Length:\\s*(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val headerEnd = text.indexOf("\r\n\r\n") + 4
                    val bodyReceived = current.size - headerEnd
                    if (bodyReceived >= cl) break
                }
            }
        } catch (_: Exception) {}
        return allData.toByteArray()
    }

    private fun String.hexToByteArraySafe(): ByteArray {
        val clean = this.trim().replace(" ", "").replace(":", "")
        if (clean.length < 2) return ByteArray(0)
        return try {
            ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: Exception) { ByteArray(0) }
    }
}
