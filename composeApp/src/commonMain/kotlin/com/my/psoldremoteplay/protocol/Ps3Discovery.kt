package com.my.psoldremoteplay.protocol

data class Ps3Info(
    val ip: String,
    val mac: ByteArray,
    val nickname: String,
    val npxId: ByteArray
) {
    val macString: String get() = mac.joinToString(":") { "%02X".format(it) }
}

interface Ps3Discoverer {
    suspend fun discover(timeoutMs: Int = 3000): Ps3Info?
    suspend fun discoverDirect(ip: String, timeoutMs: Int = 3000): Ps3Info?
}
