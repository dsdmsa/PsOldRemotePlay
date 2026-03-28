package com.my.psoldremoteplay.protocol

interface VideoRenderer {
    fun start()
    fun onStreamPacket(header: ByteArray, payload: ByteArray, isEncrypted: Boolean)
    fun stop()
}
