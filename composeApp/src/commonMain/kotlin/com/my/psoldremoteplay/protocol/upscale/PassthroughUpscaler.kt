package com.my.psoldremoteplay.protocol.upscale

import com.my.psoldremoteplay.protocol.codec.VideoFrame

/**
 * No-op upscaler that passes frames through unchanged. Use as default when upscaling is disabled.
 */
class PassthroughUpscaler : UpscaleFilter {
    override suspend fun process(frame: VideoFrame): VideoFrame = frame
}
