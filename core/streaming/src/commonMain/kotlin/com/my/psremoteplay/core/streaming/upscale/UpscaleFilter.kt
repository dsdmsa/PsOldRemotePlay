package com.my.psremoteplay.core.streaming.upscale

import com.my.psremoteplay.core.streaming.codec.VideoFrame

interface UpscaleFilter {
    suspend fun process(frame: VideoFrame): VideoFrame
}
