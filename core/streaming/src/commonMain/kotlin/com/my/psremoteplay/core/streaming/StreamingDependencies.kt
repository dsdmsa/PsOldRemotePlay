package com.my.psremoteplay.core.streaming

import com.my.psremoteplay.core.streaming.audio.AudioRenderer
import com.my.psremoteplay.core.streaming.codec.AudioDecoder
import com.my.psremoteplay.core.streaming.codec.VideoDecoder
import com.my.psremoteplay.core.streaming.upscale.UpscaleFilter
import com.my.psremoteplay.core.streaming.video.VideoRenderer

interface StreamingDependencies {
    val crypto: Crypto
    val videoDecoder: VideoDecoder
    val audioDecoder: AudioDecoder
    val videoRenderer: VideoRenderer
    val audioRenderer: AudioRenderer
    val upscaleFilter: UpscaleFilter
    val logger: Logger
}
