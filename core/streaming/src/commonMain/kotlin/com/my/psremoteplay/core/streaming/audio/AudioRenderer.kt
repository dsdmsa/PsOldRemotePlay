package com.my.psremoteplay.core.streaming.audio

import com.my.psremoteplay.core.streaming.codec.AudioBuffer

interface AudioRenderer {
    suspend fun start()
    suspend fun onAudioBuffer(buffer: AudioBuffer)
    suspend fun stop()
}
