package com.my.psremoteplay.feature.ps2.protocol

import com.my.psremoteplay.core.streaming.input.ControllerState

object Ps2Protocol {

    const val DEFAULT_PORT = 9295

    // Message types
    const val VIDEO_FRAME: Byte = 0x01
    const val CONTROLLER_STATE: Byte = 0x03
    const val SERVER_INFO: Byte = 0x10
    const val CLIENT_HELLO: Byte = 0x20

    const val CONTROLLER_PAYLOAD_SIZE = 14

    /**
     * Builds a wire frame: 4-byte BE length (type + payload size) + type byte + payload.
     */
    fun buildFrame(type: Byte, payload: ByteArray): ByteArray {
        val frameLength = 1 + payload.size // type + payload
        val frame = ByteArray(4 + frameLength)
        // 4-byte big-endian length
        frame[0] = (frameLength shr 24 and 0xFF).toByte()
        frame[1] = (frameLength shr 16 and 0xFF).toByte()
        frame[2] = (frameLength shr 8 and 0xFF).toByte()
        frame[3] = (frameLength and 0xFF).toByte()
        // type
        frame[4] = type
        // payload
        payload.copyInto(frame, destinationOffset = 5)
        return frame
    }

    /**
     * Encodes a ControllerState into a 14-byte payload:
     *   bytes 0-3:  buttons as 4-byte BE int
     *   bytes 4-5:  leftStickX  as 2-byte BE short (-1.0..1.0 -> -32768..32767)
     *   bytes 6-7:  leftStickY  as 2-byte BE short
     *   bytes 8-9:  rightStickX as 2-byte BE short
     *   bytes 10-11: rightStickY as 2-byte BE short
     *   byte 12:    l2 as byte (0..255)
     *   byte 13:    r2 as byte (0..255)
     */
    fun encodeControllerState(state: ControllerState): ByteArray {
        val data = ByteArray(CONTROLLER_PAYLOAD_SIZE)
        val b = state.buttons
        data[0] = (b shr 24 and 0xFF).toByte()
        data[1] = (b shr 16 and 0xFF).toByte()
        data[2] = (b shr 8 and 0xFF).toByte()
        data[3] = (b and 0xFF).toByte()

        fun floatToShort(v: Float): Short =
            (v.coerceIn(-1f, 1f) * 32767f).toInt().toShort()

        val lx = floatToShort(state.leftStickX)
        val ly = floatToShort(state.leftStickY)
        val rx = floatToShort(state.rightStickX)
        val ry = floatToShort(state.rightStickY)

        data[4] = (lx.toInt() shr 8 and 0xFF).toByte()
        data[5] = (lx.toInt() and 0xFF).toByte()
        data[6] = (ly.toInt() shr 8 and 0xFF).toByte()
        data[7] = (ly.toInt() and 0xFF).toByte()
        data[8] = (rx.toInt() shr 8 and 0xFF).toByte()
        data[9] = (rx.toInt() and 0xFF).toByte()
        data[10] = (ry.toInt() shr 8 and 0xFF).toByte()
        data[11] = (ry.toInt() and 0xFF).toByte()

        data[12] = (state.l2.coerceIn(0f, 1f) * 255f).toInt().toByte()
        data[13] = (state.r2.coerceIn(0f, 1f) * 255f).toInt().toByte()

        return data
    }

    /**
     * Decodes a 14-byte payload back into a ControllerState.
     */
    fun decodeControllerState(bytes: ByteArray): ControllerState {
        require(bytes.size >= CONTROLLER_PAYLOAD_SIZE) {
            "Expected at least $CONTROLLER_PAYLOAD_SIZE bytes, got ${bytes.size}"
        }

        val buttons = (bytes[0].toInt() and 0xFF shl 24) or
                (bytes[1].toInt() and 0xFF shl 16) or
                (bytes[2].toInt() and 0xFF shl 8) or
                (bytes[3].toInt() and 0xFF)

        fun readShort(offset: Int): Short =
            ((bytes[offset].toInt() and 0xFF shl 8) or (bytes[offset + 1].toInt() and 0xFF)).toShort()

        fun shortToFloat(s: Short): Float = s.toFloat() / 32767f

        return ControllerState(
            buttons = buttons,
            leftStickX = shortToFloat(readShort(4)),
            leftStickY = shortToFloat(readShort(6)),
            rightStickX = shortToFloat(readShort(8)),
            rightStickY = shortToFloat(readShort(10)),
            l2 = (bytes[12].toInt() and 0xFF) / 255f,
            r2 = (bytes[13].toInt() and 0xFF) / 255f
        )
    }
}

/**
 * Accumulates bytes and extracts complete frames.
 *
 * Wire format per frame: 4-byte BE length header + (type byte + payload).
 * The length header encodes the size of type + payload (i.e. not including itself).
 */
class FrameReader {

    private var buffer = ByteArray(4096)
    private var writePos = 0

    /**
     * Feed raw bytes into the reader.
     */
    fun feed(data: ByteArray, offset: Int = 0, length: Int = data.size) {
        ensureCapacity(writePos + length)
        data.copyInto(buffer, destinationOffset = writePos, startIndex = offset, endIndex = offset + length)
        writePos += length
    }

    /**
     * Returns the next complete frame as (type, payload), or null if not enough data yet.
     */
    fun poll(): Pair<Byte, ByteArray>? {
        if (writePos < 4) return null

        val frameLength = (buffer[0].toInt() and 0xFF shl 24) or
                (buffer[1].toInt() and 0xFF shl 16) or
                (buffer[2].toInt() and 0xFF shl 8) or
                (buffer[3].toInt() and 0xFF)

        if (frameLength <= 0) {
            // Corrupt data -- skip the 4-byte header
            compact(4)
            return null
        }

        val totalNeeded = 4 + frameLength // header + body
        if (writePos < totalNeeded) return null

        val type = buffer[4]
        val payload = buffer.copyOfRange(5, 4 + frameLength)

        compact(totalNeeded)
        return Pair(type, payload)
    }

    private fun compact(consumed: Int) {
        val remaining = writePos - consumed
        if (remaining > 0) {
            buffer.copyInto(buffer, destinationOffset = 0, startIndex = consumed, endIndex = writePos)
        }
        writePos = remaining
    }

    private fun ensureCapacity(needed: Int) {
        if (needed <= buffer.size) return
        var newSize = buffer.size
        while (newSize < needed) newSize *= 2
        val newBuffer = ByteArray(newSize)
        buffer.copyInto(newBuffer)
        buffer = newBuffer
    }
}
