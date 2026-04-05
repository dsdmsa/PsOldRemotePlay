package com.my.psremoteplay.core.audio

import com.my.psremoteplay.core.model.codec.AudioBuffer

interface AudioRenderer {
    suspend fun start()
    suspend fun onAudioBuffer(buffer: AudioBuffer)
    suspend fun stop()
}
