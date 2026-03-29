package com.my.psoldremoteplay.protocol.upscale

import com.my.psoldremoteplay.protocol.codec.VideoFrame

/**
 * Post-process filter for video frames. Can be used for upscaling, denoising, etc.
 */
interface UpscaleFilter {
    /**
     * Process a video frame and return the result.
     * May return the same frame, a modified copy, or a new frame with different dimensions.
     */
    suspend fun process(frame: VideoFrame): VideoFrame
}
