package com.my.psremoteplay.core.upscale

import com.my.psremoteplay.core.model.codec.VideoFrame

class PassthroughUpscaler : UpscaleFilter {
    override suspend fun process(frame: VideoFrame): VideoFrame = frame
}
