package com.my.psremoteplay.core.audio.stubs

import com.my.psremoteplay.core.audio.AudioRenderer
import com.my.psremoteplay.core.model.Logger
import com.my.psremoteplay.core.model.codec.AudioBuffer

class StubAudioRenderer(private val logger: Logger) : AudioRenderer {
    override suspend fun start() {
        logger.log("AUDIO", "Audio renderer started")
    }

    override suspend fun onAudioBuffer(buffer: AudioBuffer) {
        logger.log("AUDIO", "Audio buffer: ${buffer.samples.size} bytes @ ${buffer.sampleRate}Hz")
    }

    override suspend fun stop() {
        logger.log("AUDIO", "Audio renderer stopped")
    }
}
