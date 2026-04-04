package com.my.psremoteplay.core.streaming.upscale

import com.my.psremoteplay.core.streaming.codec.VideoFrame

class PassthroughUpscaler : UpscaleFilter {
    override suspend fun process(frame: VideoFrame): VideoFrame = frame
}
