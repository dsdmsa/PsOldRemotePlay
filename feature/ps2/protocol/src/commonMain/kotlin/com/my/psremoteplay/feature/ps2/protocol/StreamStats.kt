package com.my.psremoteplay.feature.ps2.protocol

/**
 * Client streaming statistics for adaptive bitrate control.
 * Sent periodically from client to server over the control channel.
 */
data class StreamStats(
    val framesReceived: Long = 0,
    val framesDecoded: Long = 0,
    val framesDropped: Long = 0,
    val avgDecodeMs: Long = 0,
    val avgNetworkJitterMs: Long = 0,
    val currentFps: Int = 0
) {
    fun encode(): ByteArray {
        val data = ByteArray(PAYLOAD_SIZE)
        fun putInt(offset: Int, value: Int) {
            data[offset] = (value ushr 24 and 0xFF).toByte()
            data[offset + 1] = (value ushr 16 and 0xFF).toByte()
            data[offset + 2] = (value ushr 8 and 0xFF).toByte()
            data[offset + 3] = (value and 0xFF).toByte()
        }
        putInt(0, framesReceived.toInt())
        putInt(4, framesDecoded.toInt())
        putInt(8, framesDropped.toInt())
        putInt(12, avgDecodeMs.toInt())
        putInt(16, avgNetworkJitterMs.toInt())
        putInt(20, currentFps)
        return data
    }

    companion object {
        const val PAYLOAD_SIZE = 24

        fun decode(data: ByteArray): StreamStats {
            require(data.size >= PAYLOAD_SIZE)
            fun readInt(offset: Int): Int =
                ((data[offset].toInt() and 0xFF) shl 24) or
                        ((data[offset + 1].toInt() and 0xFF) shl 16) or
                        ((data[offset + 2].toInt() and 0xFF) shl 8) or
                        (data[offset + 3].toInt() and 0xFF)
            return StreamStats(
                framesReceived = readInt(0).toLong(),
                framesDecoded = readInt(4).toLong(),
                framesDropped = readInt(8).toLong(),
                avgDecodeMs = readInt(12).toLong(),
                avgNetworkJitterMs = readInt(16).toLong(),
                currentFps = readInt(20)
            )
        }
    }
}
