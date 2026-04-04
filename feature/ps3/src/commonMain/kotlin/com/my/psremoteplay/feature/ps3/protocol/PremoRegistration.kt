package com.my.psremoteplay.feature.ps3.protocol

data class RegistrationResult(
    val pkey: ByteArray,
    val ps3Mac: String,
    val ps3Nickname: String,
    val deviceId: ByteArray,
    val deviceMac: ByteArray
)

interface PremoRegistration {
    suspend fun register(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String = "PsOldRemotePlay",
        platformType: Int = 1
    ): Result<RegistrationResult>
}
