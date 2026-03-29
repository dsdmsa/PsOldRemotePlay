package com.my.psoldremoteplay.protocol

import com.my.psoldremoteplay.protocol.codec.AudioBuffer
import kotlinx.coroutines.flow.StateFlow

/**
 * Audio playback handler. Outputs decoded PCM to speakers.
 */
interface AudioRenderer {
    /**
     * Initialize audio playback.
     */
    suspend fun start()

    /**
     * Queue audio for playback. Implementation may buffer/batch internally.
     */
    suspend fun onAudioBuffer(buffer: AudioBuffer)

    /**
     * Stop playback and release resources.
     */
    suspend fun stop()
}
