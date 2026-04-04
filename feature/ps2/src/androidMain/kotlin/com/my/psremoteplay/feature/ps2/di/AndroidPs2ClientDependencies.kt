package com.my.psremoteplay.feature.ps2.di

import com.my.psremoteplay.core.streaming.AndroidVideoRenderer
import com.my.psremoteplay.core.streaming.Crypto
import com.my.psremoteplay.core.streaming.JvmCrypto
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.StreamingDependencies
import com.my.psremoteplay.core.streaming.audio.AudioRenderer
import com.my.psremoteplay.core.streaming.codec.AudioDecoder
import com.my.psremoteplay.core.streaming.codec.VideoDecoder
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.core.streaming.stubs.StubAudioDecoder
import com.my.psremoteplay.core.streaming.stubs.StubAudioRenderer
import com.my.psremoteplay.core.streaming.stubs.StubVideoDecoder
import com.my.psremoteplay.core.streaming.upscale.PassthroughUpscaler
import com.my.psremoteplay.core.streaming.upscale.UpscaleFilter
import com.my.psremoteplay.core.streaming.video.VideoRenderer
import com.my.psremoteplay.feature.ps2.protocol.Ps2Protocol
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.StreamingPreset
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamClient
import com.my.psremoteplay.feature.ps2.strategy.client.AndroidJavaCvClient
import com.my.psremoteplay.feature.ps2.strategy.client.AndroidUdpJpegClient
import java.io.DataInputStream
import java.net.Socket

class AndroidPs2ClientDependencies(
    preset: StreamingPreset = StreamingPreset.JPEG_UDP
) : Ps2ClientDependencies {

    private val _logger: Logger = object : Logger {
        override fun log(tag: String, message: String) { android.util.Log.d(tag, message) }
        override fun error(tag: String, message: String, throwable: Throwable?) {
            android.util.Log.e(tag, message, throwable)
        }
    }

    override val streaming: StreamingDependencies = object : StreamingDependencies {
        override val crypto: Crypto = JvmCrypto
        override val videoDecoder: VideoDecoder = StubVideoDecoder()
        override val audioDecoder: AudioDecoder = StubAudioDecoder()
        override val videoRenderer: VideoRenderer = AndroidVideoRenderer(_logger)
        override val audioRenderer: AudioRenderer = StubAudioRenderer(_logger)
        override val upscaleFilter: UpscaleFilter = PassthroughUpscaler()
        override val logger: Logger = _logger
    }

    override val logger: Logger = _logger
    override val streamConfig = StreamConfig()

    override val videoStreamClient: VideoStreamClient = createClientStrategy(preset)

    private fun createClientStrategy(preset: StreamingPreset): VideoStreamClient = when (preset) {
        StreamingPreset.JPEG_TCP -> AndroidUdpJpegClient(_logger) // fallback to UDP
        StreamingPreset.JPEG_UDP -> AndroidUdpJpegClient(_logger)
        StreamingPreset.H264_RTP -> AndroidJavaCvClient(_logger, "rtp")
        StreamingPreset.H264_MPEGTS -> AndroidJavaCvClient(_logger, "mpegts")
        StreamingPreset.PCSX2_PIPE -> AndroidUdpJpegClient(_logger) // PCSX2 pipe sends JPEG via UDP
    }

    private var controlSocket: Socket? = null

    override fun connectControl(ip: String, port: Int, onServerInfo: (String) -> Unit): Boolean {
        return try {
            val s = Socket(ip, port)
            controlSocket = s
            val helloPayload = """{"name":"Android Client"}""".toByteArray()
            val helloFrame = Ps2Protocol.buildFrame(Ps2Protocol.CLIENT_HELLO, helloPayload)
            s.getOutputStream().write(helloFrame)
            s.getOutputStream().flush()

            Thread {
                try {
                    val input = DataInputStream(s.getInputStream())
                    while (!s.isClosed) {
                        val length = input.readInt()
                        if (length <= 0) continue
                        val type = input.readByte()
                        val payloadSize = length - 1
                        val payload = ByteArray(payloadSize)
                        if (payloadSize > 0) input.readFully(payload)
                        when (type) {
                            Ps2Protocol.SERVER_INFO -> onServerInfo(String(payload))
                        }
                    }
                } catch (e: Exception) {
                    if (!s.isClosed) _logger.error("CONTROL", "Reader error", e)
                }
            }.apply { isDaemon = true; name = "control-reader"; start() }

            _logger.log("CONTROL", "Connected to $ip:$port")
            true
        } catch (e: Exception) {
            _logger.error("CONTROL", "Connect failed: ${e.message}", e)
            false
        }
    }

    override fun disconnectControl() {
        try { controlSocket?.close() } catch (_: Exception) {}
        controlSocket = null
    }

    override fun isConnected(): Boolean = controlSocket?.isClosed == false

    override fun sendControllerState(state: ControllerState) {
        val s = controlSocket ?: return
        if (s.isClosed) return
        try {
            val encoded = Ps2Protocol.encodeControllerState(state)
            val frame = Ps2Protocol.buildFrame(Ps2Protocol.CONTROLLER_STATE, encoded)
            s.getOutputStream().write(frame)
            s.getOutputStream().flush()
        } catch (e: Exception) {
            _logger.error("CONTROL", "Send failed: ${e.message}", e)
        }
    }
}
