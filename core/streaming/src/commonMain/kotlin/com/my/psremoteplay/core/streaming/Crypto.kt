package com.my.psremoteplay.core.streaming

interface Crypto {
    fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun base64Encode(data: ByteArray): String
    fun base64Decode(data: String): ByteArray
    fun randomBytes(count: Int): ByteArray
}
