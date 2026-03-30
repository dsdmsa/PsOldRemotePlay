package com.my.psoldremoteplay

import com.my.psoldremoteplay.protocol.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.Socket

/**
 * Strategy 1: Phone Type (1) with PS4 Formula
 * IV Context: PIN as 4-byte BE uint + 4 zeros
 * STATUS: TESTED - FAILED (403 Forbidden)
 * Reason: Phone type uses RANDOM key material (last 16 bytes), not PIN-derived
 */
class PhoneTypePS4FormulaStrategy : RegistrationStrategy {
    override val name = "Phone Type PS4 Formula"
    override val description = "Phone Type (1) with PIN as 4-byte BE + 4 zeros"
    override val isEnabled = false  // Already tested and failed

    override suspend fun attemptRegistration(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        crypto: PremoCrypto,
        logger: PremoLogger
    ): RegistrationAttempt = withContext(Dispatchers.IO) {
        simpleRegisterAttempt(ps3Ip, pin, deviceId, deviceMac, deviceName, 1, false, crypto, logger, name)
    }

    private fun deriveKeyPhone(keyMaterial: ByteArray): ByteArray {
        val key = ByteArray(16)
        for (i in 0..15) {
            key[i] = (((keyMaterial[i].toInt() and 0xFF) - i - 0x28) xor (PremoConstants.REG_XOR_PHONE[i].toInt() and 0xFF)).toByte()
        }
        return key
    }

    private fun deriveIvPhone(contextBytes: ByteArray): ByteArray {
        val baseIv = PremoConstants.REG_IV_PHONE.copyOf()
        for (i in 0..7) {
            baseIv[8 + i] = (baseIv[8 + i].toInt() xor contextBytes[i].toInt()).toByte()
        }
        return baseIv
    }

    private fun pinToContextBytes(pin: String): ByteArray {
        val pinInt = pin.toLongOrNull() ?: 0L
        val bytes = ByteArray(8)
        bytes[0] = (pinInt shr 24 and 0xFF).toByte()
        bytes[1] = (pinInt shr 16 and 0xFF).toByte()
        bytes[2] = (pinInt shr 8 and 0xFF).toByte()
        bytes[3] = (pinInt and 0xFF).toByte()
        return bytes
    }

    private suspend fun simpleRegisterAttempt(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        platformType: Int,
        isPinDerived: Boolean,
        crypto: PremoCrypto,
        logger: PremoLogger,
        strategyName: String
    ): RegistrationAttempt {
        return try {
            logger.log("REGIST", "▶ $strategyName")

            val bodyText = buildString {
                val clientType = when (platformType) {
                    0 -> "Phone"
                    1 -> "PC"
                    else -> "VITA"
                }
                append("Client-Type: $clientType\r\n")
                append("Client-Id: ${deviceId.joinToString("") { "%02x".format(it) }}\r\n")
                append("Client-Mac: ${deviceMac.joinToString("") { "%02x".format(it) }}\r\n")
                append("Client-Nickname: $deviceName\r\n")
            }

            val keyMaterial = if (isPinDerived) pinToKeyMaterial(pin) else crypto.randomBytes(16)
            val contextBytes = pinToContextBytes(pin)

            val aesKey = when (platformType) {
                0 -> deriveKeyPSP(keyMaterial)
                1 -> deriveKeyPhone(keyMaterial)
                else -> deriveKeyPC(keyMaterial)
            }

            val aesIv = when (platformType) {
                0 -> deriveIvPSP(contextBytes)
                1 -> deriveIvPhone(contextBytes)
                else -> deriveIvPC(contextBytes)
            }

            val plainBytes = bodyText.toByteArray(Charsets.US_ASCII)
            val paddedLen = (plainBytes.size + 15) and 0x7FFFFFF0
            val padded = ByteArray(paddedLen)
            plainBytes.copyInto(padded)

            val encrypted = crypto.aesEncrypt(padded, aesKey, aesIv)
            val fullBody = encrypted + keyMaterial

            val socket = Socket(ps3Ip, PremoConstants.PORT)
            socket.soTimeout = 10000
            val out = socket.getOutputStream()
            val httpRequest = "POST /sce/premo/regist HTTP/1.1\r\nContent-Length: ${fullBody.size}\r\n\r\n"
            out.write(httpRequest.toByteArray(Charsets.US_ASCII))
            out.write(fullBody)
            out.flush()

            val response = readFullResponse(socket.inputStream)
            val responseText = String(response, Charsets.US_ASCII)
            socket.close()

            if (responseText.contains("200 OK")) {
                logger.log("REGIST", "✅ 200 OK")
                val result = parseResponse(response, aesKey, aesIv, crypto)
                if (result != null) {
                    RegistrationAttempt(strategyName, true, result.pkey, result.ps3Mac, result.ps3Nickname)
                } else {
                    RegistrationAttempt(strategyName, false, error = "Failed to parse response")
                }
            } else if (responseText.contains("403")) {
                logger.log("REGIST", "❌ 403 Forbidden")
                RegistrationAttempt(strategyName, false, error = "403 Forbidden")
            } else {
                logger.log("REGIST", "❌ Unexpected response")
                RegistrationAttempt(strategyName, false, error = "Unexpected response")
            }
        } catch (e: Exception) {
            logger.error("REGIST", "❌ Error: ${e.message}")
            RegistrationAttempt(strategyName, false, error = e.message ?: "Unknown error")
        }
    }

    private fun deriveKeyPSP(keyMaterial: ByteArray): ByteArray {
        val key = ByteArray(16)
        for (i in 0..15) {
            key[i] = ((keyMaterial[i].toInt() and 0xFF xor (PremoConstants.REG_XOR_PSP[i].toInt() and 0xFF)) - i - 0x25).toByte()
        }
        return key
    }

    private fun deriveKeyPC(keyMaterial: ByteArray): ByteArray {
        val key = ByteArray(16)
        for (i in 0..15) {
            key[i] = ((keyMaterial[i].toInt() and 0xFF xor (PremoConstants.REG_XOR_PC[i].toInt() and 0xFF)) - i - 0x2B).toByte()
        }
        return key
    }

    private fun deriveIvPSP(contextBytes: ByteArray): ByteArray {
        val baseIv = PremoConstants.REG_IV_PSP.copyOf()
        for (i in 0..7) {
            baseIv[i] = (baseIv[i].toInt() xor contextBytes[i].toInt()).toByte()
        }
        return baseIv
    }

    private fun deriveIvPC(contextBytes: ByteArray): ByteArray {
        val baseIv = PremoConstants.REG_IV_PC.copyOf()
        for (i in 0..7) {
            baseIv[i] = (baseIv[i].toInt() xor contextBytes[i].toInt()).toByte()
        }
        return baseIv
    }

    private fun pinToKeyMaterial(pin: String): ByteArray {
        val pinInt = pin.toLongOrNull() ?: 0L
        val bytes = ByteArray(16)
        bytes[0] = (pinInt shr 24 and 0xFF).toByte()
        bytes[1] = (pinInt shr 16 and 0xFF).toByte()
        bytes[2] = (pinInt shr 8 and 0xFF).toByte()
        bytes[3] = (pinInt and 0xFF).toByte()
        return bytes
    }

    private fun parseResponse(
        rawResponse: ByteArray,
        aesKey: ByteArray,
        aesIv: ByteArray,
        crypto: PremoCrypto
    ): RegistrationResult? {
        val headerEnd = findHeaderEnd(rawResponse) ?: return null
        val bodyStart = headerEnd + 4
        val body = rawResponse.copyOfRange(bodyStart, rawResponse.size)

        return try {
            val decrypted = crypto.aesDecrypt(body, aesKey, aesIv)
            val decryptedText = String(decrypted, Charsets.US_ASCII)

            val fields = mutableMapOf<String, String>()
            for (line in decryptedText.split("\r\n", "\n")) {
                val sep = line.indexOf(':')
                if (sep > 0) {
                    fields[line.substring(0, sep).trim()] = line.substring(sep + 1).trim()
                }
            }

            val pkeyHex = fields["PREMO-Key"] ?: ""
            val pkeyBytes = hexToByteArray(pkeyHex)

            RegistrationResult(
                pkey = pkeyBytes,
                ps3Mac = fields["PS3-Mac"] ?: "",
                ps3Nickname = fields["PS3-Nickname"] ?: "",
                deviceId = ByteArray(16),
                deviceMac = ByteArray(6)
            )
        } catch (e: Exception) {
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
        val allData = mutableListOf<Byte>()
        try {
            while (true) {
                val n = inp.read(buf)
                if (n <= 0) break
                for (i in 0 until n) allData.add(buf[i])
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

    private fun hexToByteArray(hex: String): ByteArray {
        val clean = hex.trim().replace(" ", "").replace(":", "")
        if (clean.length < 2) return ByteArray(0)
        return try {
            ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: Exception) {
            ByteArray(0)
        }
    }
}

/**
 * Strategy 2: PC Type (2) with PS4 Formula
 * IV Context: PIN as 4-byte BE uint + 4 zeros
 * Key Material: PIN as 4-byte BE + 12 zeros (PIN-derived)
 * Body Structure: 480-byte prefix + encrypted portion at offset 0x1E0
 * STATUS: NEW - From VAIO DLL Analysis
 *
 * Insight: VRPSDK.dll analysis shows PC type uses PIN-derived key material,
 * unlike Phone type which uses random material. Encrypted portion starts at
 * offset 0x1E0 (480 bytes), not byte 0. Client-Type should be "VITA".
 */
class PCTypePS4FormulaStrategy : RegistrationStrategy {
    override val name = "PC Type PS4 Formula"
    override val description = "PC Type (2) with PIN-derived key material (4-byte BE + 12 zeros) and 480-byte body prefix"
    override val isEnabled = true  // HIGH PRIORITY - new approach

    override suspend fun attemptRegistration(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        crypto: PremoCrypto,
        logger: PremoLogger
    ): RegistrationAttempt = withContext(Dispatchers.IO) {
        try {
            logger.log("REGIST", "▶ $name")

            val bodyText = buildString {
                append("Client-Type: VITA\r\n")
                append("Client-Id: ${deviceId.joinToString("") { "%02x".format(it) }}\r\n")
                append("Client-Mac: ${deviceMac.joinToString("") { "%02x".format(it) }}\r\n")
                append("Client-Nickname: $deviceName\r\n")
            }

            val keyMaterial = pinToKeyMaterial(pin)
            val contextBytes = pinToContextBytes(pin)

            val aesKey = deriveKeyPC(keyMaterial)
            val aesIv = deriveIvPC(contextBytes)

            val plainBytes = bodyText.toByteArray(Charsets.US_ASCII)
            val paddedLen = (plainBytes.size + 15) and 0x7FFFFFF0

            // Encrypt plaintext body
            val padded = ByteArray(paddedLen)
            plainBytes.copyInto(padded)
            val encrypted = crypto.aesEncrypt(padded, aesKey, aesIv)

            // Build body: 480-byte zero prefix + encrypted data + key material
            val prefix = ByteArray(480)  // Unknown prefix content, use zeros
            val bodyWithKeyMaterial = prefix + encrypted + keyMaterial

            val socket = Socket(ps3Ip, PremoConstants.PORT)
            socket.soTimeout = 10000
            val out = socket.getOutputStream()
            val httpRequest = "POST /sce/premo/regist HTTP/1.1\r\nContent-Length: ${bodyWithKeyMaterial.size}\r\n\r\n"
            out.write(httpRequest.toByteArray(Charsets.US_ASCII))
            out.write(bodyWithKeyMaterial)
            out.flush()

            val response = readFullResponse(socket.inputStream)
            val responseText = String(response, Charsets.US_ASCII)
            socket.close()

            if (responseText.contains("200 OK")) {
                logger.log("REGIST", "✅ 200 OK")
                val result = parseResponse(response, aesKey, aesIv, crypto)
                if (result != null) {
                    RegistrationAttempt(name, true, result.pkey, result.ps3Mac, result.ps3Nickname)
                } else {
                    RegistrationAttempt(name, false, error = "Failed to parse response")
                }
            } else if (responseText.contains("403")) {
                logger.log("REGIST", "❌ 403 Forbidden")
                RegistrationAttempt(name, false, error = "403 Forbidden")
            } else {
                logger.log("REGIST", "❌ Unexpected response")
                RegistrationAttempt(name, false, error = "Unexpected response")
            }
        } catch (e: Exception) {
            logger.error("REGIST", "❌ Error: ${e.message}")
            RegistrationAttempt(name, false, error = e.message ?: "Unknown error")
        }
    }

    private fun deriveKeyPC(keyMaterial: ByteArray): ByteArray {
        val key = ByteArray(16)
        for (i in 0..15) {
            key[i] = ((keyMaterial[i].toInt() and 0xFF xor (PremoConstants.REG_XOR_PC[i].toInt() and 0xFF)) - i - 0x2B).toByte()
        }
        return key
    }

    private fun deriveIvPC(contextBytes: ByteArray): ByteArray {
        val baseIv = PremoConstants.REG_IV_PC.copyOf()
        for (i in 0..7) {
            baseIv[i] = (baseIv[i].toInt() xor contextBytes[i].toInt()).toByte()
        }
        return baseIv
    }

    private fun pinToKeyMaterial(pin: String): ByteArray {
        val pinInt = pin.toLongOrNull() ?: 0L
        val bytes = ByteArray(16)
        bytes[0] = (pinInt shr 24 and 0xFF).toByte()
        bytes[1] = (pinInt shr 16 and 0xFF).toByte()
        bytes[2] = (pinInt shr 8 and 0xFF).toByte()
        bytes[3] = (pinInt and 0xFF).toByte()
        return bytes
    }

    private fun pinToContextBytes(pin: String): ByteArray {
        val pinInt = pin.toLongOrNull() ?: 0L
        val bytes = ByteArray(8)
        bytes[0] = (pinInt shr 24 and 0xFF).toByte()
        bytes[1] = (pinInt shr 16 and 0xFF).toByte()
        bytes[2] = (pinInt shr 8 and 0xFF).toByte()
        bytes[3] = (pinInt and 0xFF).toByte()
        return bytes
    }

    private fun parseResponse(
        rawResponse: ByteArray,
        aesKey: ByteArray,
        aesIv: ByteArray,
        crypto: PremoCrypto
    ): RegistrationResult? {
        val headerEnd = findHeaderEnd(rawResponse) ?: return null
        val bodyStart = headerEnd + 4
        val body = rawResponse.copyOfRange(bodyStart, rawResponse.size)

        return try {
            val decrypted = crypto.aesDecrypt(body, aesKey, aesIv)
            val decryptedText = String(decrypted, Charsets.US_ASCII)

            val fields = mutableMapOf<String, String>()
            for (line in decryptedText.split("\r\n", "\n")) {
                val sep = line.indexOf(':')
                if (sep > 0) {
                    fields[line.substring(0, sep).trim()] = line.substring(sep + 1).trim()
                }
            }

            val pkeyHex = fields["PREMO-Key"] ?: ""
            val pkeyBytes = hexToByteArray(pkeyHex)

            RegistrationResult(
                pkey = pkeyBytes,
                ps3Mac = fields["PS3-Mac"] ?: "",
                ps3Nickname = fields["PS3-Nickname"] ?: "",
                deviceId = ByteArray(16),
                deviceMac = ByteArray(6)
            )
        } catch (e: Exception) {
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
        val allData = mutableListOf<Byte>()
        try {
            while (true) {
                val n = inp.read(buf)
                if (n <= 0) break
                for (i in 0 until n) allData.add(buf[i])
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

    private fun hexToByteArray(hex: String): ByteArray {
        val clean = hex.trim().replace(" ", "").replace(":", "")
        if (clean.length < 2) return ByteArray(0)
        return try {
            ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: Exception) {
            ByteArray(0)
        }
    }
}

/**
 * Strategy 3: PSP Type (0) with PS4 Formula
 * IV Context: PIN as 4-byte BE uint + 4 zeros
 * Key Material: Random 16 bytes
 * STATUS: NEW - Never tested before
 *
 * This is the third major registration type from VRPSDK.dll.
 * PSP uses different key derivation: (km ^ XOR_PSP) - i - 0x25
 */
class PSPTypePS4FormulaStrategy : RegistrationStrategy {
    override val name = "PSP Type PS4 Formula"
    override val description = "PSP Type (0) with PIN as 4-byte BE + 4 zeros"
    override val isEnabled = true  // NEW - Never tested yet

    override suspend fun attemptRegistration(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        crypto: PremoCrypto,
        logger: PremoLogger
    ): RegistrationAttempt = withContext(Dispatchers.IO) {
        try {
            logger.log("REGIST", "▶ $name")

            val bodyText = buildString {
                append("Client-Type: Phone\r\n")  // PSP type uses "Phone" client type
                append("Client-Id: ${deviceId.joinToString("") { "%02x".format(it) }}\r\n")
                append("Client-Mac: ${deviceMac.joinToString("") { "%02x".format(it) }}\r\n")
                append("Client-Nickname: $deviceName\r\n")
            }

            val keyMaterial = crypto.randomBytes(16)  // PSP uses random key material
            val contextBytes = pinToContextBytes(pin)

            val aesKey = deriveKeyPSP(keyMaterial)
            val aesIv = deriveIvPSP(contextBytes)

            val plainBytes = bodyText.toByteArray(Charsets.US_ASCII)
            val paddedLen = (plainBytes.size + 15) and 0x7FFFFFF0
            val padded = ByteArray(paddedLen)
            plainBytes.copyInto(padded)

            val encrypted = crypto.aesEncrypt(padded, aesKey, aesIv)
            val fullBody = encrypted + keyMaterial

            val socket = Socket(ps3Ip, PremoConstants.PORT)
            socket.soTimeout = 10000
            val out = socket.getOutputStream()
            val httpRequest = "POST /sce/premo/regist HTTP/1.1\r\nContent-Length: ${fullBody.size}\r\n\r\n"
            out.write(httpRequest.toByteArray(Charsets.US_ASCII))
            out.write(fullBody)
            out.flush()

            val response = readFullResponse(socket.inputStream)
            val responseText = String(response, Charsets.US_ASCII)
            socket.close()

            if (responseText.contains("200 OK")) {
                logger.log("REGIST", "✅ 200 OK")
                val result = parseResponse(response, aesKey, aesIv, crypto)
                if (result != null) {
                    RegistrationAttempt(name, true, result.pkey, result.ps3Mac, result.ps3Nickname)
                } else {
                    RegistrationAttempt(name, false, error = "Failed to parse response")
                }
            } else if (responseText.contains("403")) {
                logger.log("REGIST", "❌ 403 Forbidden")
                RegistrationAttempt(name, false, error = "403 Forbidden")
            } else {
                logger.log("REGIST", "❌ Unexpected response")
                RegistrationAttempt(name, false, error = "Unexpected response")
            }
        } catch (e: Exception) {
            logger.error("REGIST", "❌ Error: ${e.message}")
            RegistrationAttempt(name, false, error = e.message ?: "Unknown error")
        }
    }

    private fun deriveKeyPSP(keyMaterial: ByteArray): ByteArray {
        val key = ByteArray(16)
        for (i in 0..15) {
            key[i] = ((keyMaterial[i].toInt() and 0xFF xor (PremoConstants.REG_XOR_PSP[i].toInt() and 0xFF)) - i - 0x25).toByte()
        }
        return key
    }

    private fun deriveIvPSP(contextBytes: ByteArray): ByteArray {
        val baseIv = PremoConstants.REG_IV_PSP.copyOf()
        for (i in 0..7) {
            baseIv[i] = (baseIv[i].toInt() xor contextBytes[i].toInt()).toByte()
        }
        return baseIv
    }

    private fun pinToContextBytes(pin: String): ByteArray {
        val pinInt = pin.toLongOrNull() ?: 0L
        val bytes = ByteArray(8)
        bytes[0] = (pinInt shr 24 and 0xFF).toByte()
        bytes[1] = (pinInt shr 16 and 0xFF).toByte()
        bytes[2] = (pinInt shr 8 and 0xFF).toByte()
        bytes[3] = (pinInt and 0xFF).toByte()
        return bytes
    }

    private fun parseResponse(
        rawResponse: ByteArray,
        aesKey: ByteArray,
        aesIv: ByteArray,
        crypto: PremoCrypto
    ): RegistrationResult? {
        val headerEnd = findHeaderEnd(rawResponse) ?: return null
        val bodyStart = headerEnd + 4
        val body = rawResponse.copyOfRange(bodyStart, rawResponse.size)

        return try {
            val decrypted = crypto.aesDecrypt(body, aesKey, aesIv)
            val decryptedText = String(decrypted, Charsets.US_ASCII)

            val fields = mutableMapOf<String, String>()
            for (line in decryptedText.split("\r\n", "\n")) {
                val sep = line.indexOf(':')
                if (sep > 0) {
                    fields[line.substring(0, sep).trim()] = line.substring(sep + 1).trim()
                }
            }

            val pkeyHex = fields["PREMO-Key"] ?: ""
            val pkeyBytes = hexToByteArray(pkeyHex)

            RegistrationResult(
                pkey = pkeyBytes,
                ps3Mac = fields["PS3-Mac"] ?: "",
                ps3Nickname = fields["PS3-Nickname"] ?: "",
                deviceId = ByteArray(16),
                deviceMac = ByteArray(6)
            )
        } catch (e: Exception) {
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
        val allData = mutableListOf<Byte>()
        try {
            while (true) {
                val n = inp.read(buf)
                if (n <= 0) break
                for (i in 0 until n) allData.add(buf[i])
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

    private fun hexToByteArray(hex: String): ByteArray {
        val clean = hex.trim().replace(" ", "").replace(":", "")
        if (clean.length < 2) return ByteArray(0)
        return try {
            ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: Exception) {
            ByteArray(0)
        }
    }
}

/**
 * Strategy 4: Phone Type with 8-Byte BE Longlong
 * IV Context: PIN as 8-byte big-endian longlong
 * STATUS: TESTED - FAILED
 */
class PhoneTypeBELonglongStrategy : RegistrationStrategy {
    override val name = "Phone Type BE Longlong"
    override val description = "Phone Type (1) with PIN as 8-byte BE longlong"
    override val isEnabled = false

    override suspend fun attemptRegistration(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        crypto: PremoCrypto,
        logger: PremoLogger
    ): RegistrationAttempt = withContext(Dispatchers.IO) {
        logger.log("REGIST", "▶ $name (disabled)")
        RegistrationAttempt(name, false, error = "Disabled - already tested and failed")
    }
}

/**
 * Strategy 4: Phone Type with 8-Byte LE Longlong
 * IV Context: PIN as 8-byte little-endian longlong
 * STATUS: TESTED - FAILED
 */
class PhoneTypeLELonglongStrategy : RegistrationStrategy {
    override val name = "Phone Type LE Longlong"
    override val description = "Phone Type (1) with PIN as 8-byte LE longlong"
    override val isEnabled = false

    override suspend fun attemptRegistration(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        crypto: PremoCrypto,
        logger: PremoLogger
    ): RegistrationAttempt = withContext(Dispatchers.IO) {
        logger.log("REGIST", "▶ $name (disabled)")
        RegistrationAttempt(name, false, error = "Disabled - already tested and failed")
    }
}
