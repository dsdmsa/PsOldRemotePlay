package com.my.psoldremoteplay

import com.my.psoldremoteplay.protocol.*

/**
 * Strategy interface for different registration approaches.
 * Allows testing multiple IV context encodings without removing old attempts.
 */
interface RegistrationStrategy {
    /** Human-readable name for this approach */
    val name: String

    /** Description of what this strategy tests */
    val description: String

    /** Whether this strategy is currently enabled for testing */
    val isEnabled: Boolean

    /**
     * Execute registration with this specific strategy.
     * Returns null if approach-specific validation fails before sending.
     */
    suspend fun attemptRegistration(
        ps3Ip: String,
        pin: String,
        deviceId: ByteArray,
        deviceMac: ByteArray,
        deviceName: String,
        crypto: PremoCrypto,
        logger: PremoLogger
    ): RegistrationAttempt
}

/**
 * Result of a registration attempt using a specific strategy.
 */
data class RegistrationAttempt(
    val strategyName: String,
    val success: Boolean,
    val pkey: ByteArray? = null,
    val ps3Mac: String = "",
    val ps3Nickname: String = "",
    val error: String? = null
) {
    fun toResult(): Result<RegistrationResult> {
        return if (success && pkey != null) {
            Result.success(RegistrationResult(pkey, ps3Mac, ps3Nickname, ByteArray(16), ByteArray(6)))
        } else {
            Result.failure(Exception(error ?: "Strategy $strategyName failed"))
        }
    }
}

/**
 * RESEARCH CONTEXT: Keep all old strategies for reference.
 * This shows what has been tried and why it failed.
 */
object RegistrationStrategies {

    /**
     * Strategy 2: Phone Type (1) with PS4 Formula
     * IV Context: PIN as 4-byte BE uint + 4 zeros
     * STATUS: TESTED - FAILED (403 Forbidden)
     *
     * Hypothesis: PS4 registration uses similar pattern, worth testing
     * Result: Does not match PS3 registration encryption
     */
    val phoneTypePS4Formula = PhoneTypePS4FormulaStrategy()

    /**
     * Strategy 3: PC Type (2) with PS4 Formula
     * IV Context: PIN as 4-byte BE uint + 4 zeros
     * Key Material: PIN as 4-byte BE + 12 zeros (PIN-derived)
     * STATUS: NEW - From VAIO DLL Analysis
     *
     * Insight: VRPSDK.dll analysis shows PC type uses PIN-derived key material,
     * unlike Phone type which uses random material. Different body structure.
     *
     * Reference: research/pupps3/ghidra_findings/22_VAIO_DLL_ANALYSIS.md page 312
     */
    val pcTypePS4Formula = PCTypePS4FormulaStrategy()

    /**
     * Strategy 4: Phone Type with 8-Byte BE Longlong
     * IV Context: PIN as 8-byte big-endian longlong
     * STATUS: TESTED - FAILED
     *
     * Initial hypothesis from code analysis. PIN traced to (longlong)(int)PIN.
     * Did not match actual PS3 encryption.
     */
    val phoneTypeBELonglong = PhoneTypeBELonglongStrategy()

    /**
     * Strategy 5: Phone Type with 8-Byte LE Longlong
     * STATUS: TESTED - FAILED
     *
     * Tested little-endian variant in case endianness was wrong.
     */
    val phoneTypeLELonglong = PhoneTypeLELonglongStrategy()

    /**
     * Add new strategies here as they are discovered/tested.
     * Keep old ones for documentation.
     */
    /**
     * Strategy 1: PSP Type (0) with PS4 Formula
     * STATUS: NEW - Never tested before
     */
    val pspTypePS4Formula = PSPTypePS4FormulaStrategy()

    /**
     * Add new strategies here as they are discovered/tested.
     * Keep old ones for documentation.
     * Test order: PSP → Phone → PC
     */
    val allStrategies: List<RegistrationStrategy> = listOf(
        pspTypePS4Formula,
        phoneTypePS4Formula,
        pcTypePS4Formula,
        phoneTypeBELonglong,
        phoneTypeLELonglong
    )
}
