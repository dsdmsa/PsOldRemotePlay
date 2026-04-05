package com.my.psremoteplay.core.upscale

import com.my.psremoteplay.core.model.codec.VideoFrame

interface UpscaleFilter {
    suspend fun process(frame: VideoFrame): VideoFrame
}
