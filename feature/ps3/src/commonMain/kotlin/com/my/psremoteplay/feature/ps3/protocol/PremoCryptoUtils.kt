package com.my.psremoteplay.feature.ps3.protocol

import com.my.psremoteplay.core.streaming.Crypto

fun xorBytes(a: ByteArray, b: ByteArray): ByteArray {
    require(a.size == b.size) { "Arrays must be same size" }
    return ByteArray(a.size) { i -> (a[i].toInt() xor b[i].toInt()).toByte() }
}

fun deriveSessionKeys(pkey: ByteArray, nonce: ByteArray): Pair<ByteArray, ByteArray> {
    val xorPkey = xorBytes(pkey, PremoConstants.SKEY0)
    val xorNonce = xorBytes(nonce, PremoConstants.SKEY2)
    return Pair(xorPkey, xorNonce)
}

fun generateAuthToken(crypto: Crypto, pkey: ByteArray, nonce: ByteArray, deviceMac: ByteArray): String {
    val (aesKey, aesIv) = deriveSessionKeys(pkey, nonce)
    val plaintext = ByteArray(16)
    deviceMac.copyInto(plaintext, 0, 0, minOf(6, deviceMac.size))
    val encrypted = crypto.aesEncrypt(plaintext, aesKey, aesIv)
    return crypto.base64Encode(encrypted)
}
