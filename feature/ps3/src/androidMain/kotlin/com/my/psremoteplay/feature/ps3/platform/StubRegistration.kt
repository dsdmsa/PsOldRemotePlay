package com.my.psremoteplay.feature.ps3.platform

import com.my.psremoteplay.feature.ps3.protocol.PremoRegistration
import com.my.psremoteplay.feature.ps3.protocol.RegistrationResult

class StubRegistration : PremoRegistration {
    override suspend fun register(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        platformType: Int
    ): Result<RegistrationResult> {
        return Result.failure(UnsupportedOperationException("Registration not available on Android"))
    }
}
