package com.my.psoldremoteplay.protocol

interface PremoCrypto {
    fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun base64Encode(data: ByteArray): String
    fun base64Decode(data: String): ByteArray
    fun randomBytes(count: Int): ByteArray
}

fun xorBytes(a: ByteArray, b: ByteArray): ByteArray {
    require(a.size == b.size) { "Arrays must be same size" }
    return ByteArray(a.size) { i -> (a[i].toInt() xor b[i].toInt()).toByte() }
}

fun deriveSessionKeys(pkey: ByteArray, nonce: ByteArray): Pair<ByteArray, ByteArray> {
    val xorPkey = xorBytes(pkey, PremoConstants.SKEY0)
    val xorNonce = xorBytes(nonce, PremoConstants.SKEY2)
    return Pair(xorPkey, xorNonce)
}

fun generateAuthToken(crypto: PremoCrypto, pkey: ByteArray, nonce: ByteArray, deviceMac: ByteArray): String {
    val (aesKey, aesIv) = deriveSessionKeys(pkey, nonce)
    val plaintext = ByteArray(16)
    deviceMac.copyInto(plaintext, 0, 0, minOf(6, deviceMac.size))
    val encrypted = crypto.aesEncrypt(plaintext, aesKey, aesIv)
    return crypto.base64Encode(encrypted)
}
