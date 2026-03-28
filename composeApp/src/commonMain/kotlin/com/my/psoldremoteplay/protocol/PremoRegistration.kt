package com.my.psoldremoteplay.protocol

data class RegistrationResult(
    val pkey: ByteArray,
    val ps3Mac: String,
    val ps3Nickname: String,
    val deviceId: ByteArray,
    val deviceMac: ByteArray
)

/**
 * Registration interface — pluggable implementation.
 *
 * Current status: The AES key derivation formula for POST /sce/premo/regist
 * is not fully reverse-engineered. Implementations:
 *   - StubRegistration: returns failure (default)
 *   - HardcodedRegistration: returns pre-extracted keys (for HEN PS3 testing)
 *   - NetworkRegistration: real protocol (once crypto is solved)
 */
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
