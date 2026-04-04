package com.my.psremoteplay.feature.ps3.platform

import com.my.psremoteplay.core.streaming.Crypto
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps3.protocol.PremoRegistration
import com.my.psremoteplay.feature.ps3.protocol.RegistrationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Strategy-based registration implementation.
 *
 * Tests multiple registration approaches (strategies) without removing old ones.
 * Each strategy is documented with:
 *   - What it tests
 *   - Why previous attempts failed
 *   - References to research findings
 *
 * For testing new ideas, simply add new strategy implementations to RegistrationStrategyImpl.kt
 * and update RegistrationStrategies.allStrategies.
 */
class JvmPremoRegistration(private val crypto: Crypto, private val logger: Logger) : PremoRegistration {

    override suspend fun register(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        platformType: Int
    ): Result<RegistrationResult> = withContext(Dispatchers.IO) {
        try {
            logger.log("REGIST", "=== REGISTRATION STRATEGY ORCHESTRATOR ===")
            logger.log("REGIST", "PS3: $ps3Ip, PIN: $pin")
            logger.log("REGIST", "Testing ${RegistrationStrategies.allStrategies.count { it.isEnabled }} enabled strategies...")
            logger.log("REGIST", "")

            val attempts = mutableListOf<RegistrationAttempt>()

            for (strategy in RegistrationStrategies.allStrategies) {
                if (!strategy.isEnabled) {
                    logger.log("REGIST", "⊘ SKIPPED: ${strategy.name} (disabled)")
                    logger.log("REGIST", "   Reason: ${strategy.description}")
                    logger.log("REGIST", "")
                    continue
                }

                logger.log("REGIST", "▶ TESTING: ${strategy.name}")
                logger.log("REGIST", "   ${strategy.description}")
                logger.log("REGIST", "")

                val attempt = strategy.attemptRegistration(
                    ps3Ip, pin, deviceId, deviceMac, deviceName,
                    crypto, logger
                )
                attempts.add(attempt)

                if (attempt.success) {
                    logger.log("REGIST", "✅ SUCCESS with ${strategy.name}!")
                    logger.log("REGIST", "PKey: ${attempt.pkey?.joinToString("") { "%02X".format(it) } ?: "null"}")
                    logger.log("REGIST", "PS3 MAC: ${attempt.ps3Mac}")
                    logger.log("REGIST", "PS3 Nickname: ${attempt.ps3Nickname}")
                    logger.log("REGIST", "")

                    return@withContext Result.success(RegistrationResult(
                        pkey = attempt.pkey ?: ByteArray(0),
                        ps3Mac = attempt.ps3Mac,
                        ps3Nickname = attempt.ps3Nickname,
                        deviceId = ByteArray(16),
                        deviceMac = ByteArray(6)
                    ))
                } else {
                    logger.log("REGIST", "❌ FAILED: ${attempt.error}")
                    logger.log("REGIST", "")
                }
            }

            logger.log("REGIST", "=== ALL STRATEGIES EXHAUSTED ===")
            logger.log("REGIST", "Summary:")
            for ((i, attempt) in attempts.withIndex()) {
                val status = if (attempt.success) "✅" else "❌"
                val error = if (!attempt.success) " — ${attempt.error}" else ""
                logger.log("REGIST", "$status ${i + 1}. ${attempt.strategyName}$error")
            }

            Result.failure(Exception("No registration strategy succeeded"))
        } catch (e: Exception) {
            logger.error("REGIST", "Registration orchestration failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
