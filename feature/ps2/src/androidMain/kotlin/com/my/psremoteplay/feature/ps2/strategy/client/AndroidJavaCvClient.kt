package com.my.psremoteplay.feature.ps2.strategy.client

import androidx.compose.ui.graphics.ImageBitmap
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Stub: JavaCV is not included on Android to avoid native lib conflicts.
 * Use AndroidUdpJpegClient (JPEG_UDP) instead.
 * Re-enable by adding bytedeco dependencies to feature:ps2 androidMain.
 */
class AndroidJavaCvClient(
    private val logger: Logger,
    private val streamFormat: String = "mpegts"
) : VideoStreamClient {
    override val name = if (streamFormat == "rtp") "H.264/RTP (unavailable)" else "H.264/MPEG-TS (unavailable)"

    override val currentFrame: StateFlow<ImageBitmap?> = MutableStateFlow(null)

    override fun start(serverIp: String, config: StreamConfig): Boolean {
        logger.error(name, "JavaCV not available on Android. Use JPEG_UDP preset instead.")
        return false
    }

    override fun stop() {}
    override fun isReceiving() = false
}
