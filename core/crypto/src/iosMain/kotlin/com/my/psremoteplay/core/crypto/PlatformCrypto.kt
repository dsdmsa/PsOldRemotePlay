package com.my.psremoteplay.core.crypto

import com.my.psremoteplay.core.model.Crypto

actual object PlatformCrypto : Crypto {
    override fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        error("Not implemented")
    }

    override fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        error("Not implemented")
    }

    override fun base64Encode(data: ByteArray): String {
        error("Not implemented")
    }

    override fun base64Decode(data: String): ByteArray {
        error("Not implemented")
    }

    override fun randomBytes(count: Int): ByteArray {
        error("Not implemented")
    }
}
