package com.my.psremoteplay.core.streaming.codec

data class VideoFrame(
    val width: Int,
    val height: Int,
    val format: String = "YUV420P",
    val planes: List<ByteArray>,
    val strides: List<Int> = emptyList(),
    val timestamp: Long = 0
)
