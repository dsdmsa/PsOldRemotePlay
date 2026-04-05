package com.my.psremoteplay.feature.ps2.protocol

/**
 * Lightweight framing protocol for H.264 NAL units over UDP.
 *
 * Each UDP datagram carries one NAL unit with a 9-byte header:
 *   [4 bytes] sequence number (uint32 BE)
 *   [4 bytes] timestamp in milliseconds (uint32 BE)
 *   [1 byte]  flags (bit 0 = keyframe/IDR, bit 1 = SPS, bit 2 = PPS)
 *   [N bytes] NAL unit data (Annex B format with 00 00 00 01 start code)
 */
object NalUnitProtocol {
    const val HEADER_SIZE = 9

    const val FLAG_KEYFRAME: Int = 0x01
    const val FLAG_SPS: Int = 0x02
    const val FLAG_PPS: Int = 0x04

    private const val NAL_TYPE_IDR = 5
    private const val NAL_TYPE_SPS = 7
    private const val NAL_TYPE_PPS = 8

    fun wrapNalUnit(nalUnit: ByteArray, sequenceNumber: Int, timestampMs: Long): ByteArray {
        val flags = detectFlags(nalUnit)
        val packet = ByteArray(HEADER_SIZE + nalUnit.size)

        packet[0] = (sequenceNumber ushr 24 and 0xFF).toByte()
        packet[1] = (sequenceNumber ushr 16 and 0xFF).toByte()
        packet[2] = (sequenceNumber ushr 8 and 0xFF).toByte()
        packet[3] = (sequenceNumber and 0xFF).toByte()

        val ts = timestampMs.toInt()
        packet[4] = (ts ushr 24 and 0xFF).toByte()
        packet[5] = (ts ushr 16 and 0xFF).toByte()
        packet[6] = (ts ushr 8 and 0xFF).toByte()
        packet[7] = (ts and 0xFF).toByte()

        packet[8] = flags.toByte()

        nalUnit.copyInto(packet, HEADER_SIZE)
        return packet
    }

    fun unwrapPacket(data: ByteArray, length: Int): NalPacket? {
        if (length < HEADER_SIZE + 1) return null

        val seq = ((data[0].toInt() and 0xFF) shl 24) or
                ((data[1].toInt() and 0xFF) shl 16) or
                ((data[2].toInt() and 0xFF) shl 8) or
                (data[3].toInt() and 0xFF)

        val ts = ((data[4].toInt() and 0xFF) shl 24) or
                ((data[5].toInt() and 0xFF) shl 16) or
                ((data[6].toInt() and 0xFF) shl 8) or
                (data[7].toInt() and 0xFF)

        val flags = data[8].toInt() and 0xFF

        val nalData = data.copyOfRange(HEADER_SIZE, length)
        return NalPacket(seq, ts.toLong() and 0xFFFFFFFFL, flags, nalData)
    }

    private fun detectFlags(nalUnit: ByteArray): Int {
        val nalTypeOffset = findNalTypeOffset(nalUnit)
        if (nalTypeOffset < 0 || nalTypeOffset >= nalUnit.size) return 0

        val nalType = nalUnit[nalTypeOffset].toInt() and 0x1F
        return when (nalType) {
            NAL_TYPE_IDR -> FLAG_KEYFRAME
            NAL_TYPE_SPS -> FLAG_SPS
            NAL_TYPE_PPS -> FLAG_PPS
            else -> 0
        }
    }

    private fun findNalTypeOffset(data: ByteArray): Int {
        if (data.size < 4) return -1
        if (data[0] == 0.toByte() && data[1] == 0.toByte() &&
            data[2] == 0.toByte() && data[3] == 1.toByte()
        ) return 4
        if (data.size >= 3 && data[0] == 0.toByte() &&
            data[1] == 0.toByte() && data[2] == 1.toByte()
        ) return 3
        return 0
    }
}

data class NalPacket(
    val sequenceNumber: Int,
    val timestampMs: Long,
    val flags: Int,
    val nalData: ByteArray
) {
    val isKeyframe: Boolean get() = flags and NalUnitProtocol.FLAG_KEYFRAME != 0
    val isSps: Boolean get() = flags and NalUnitProtocol.FLAG_SPS != 0
    val isPps: Boolean get() = flags and NalUnitProtocol.FLAG_PPS != 0
    val isConfig: Boolean get() = isSps || isPps
}
