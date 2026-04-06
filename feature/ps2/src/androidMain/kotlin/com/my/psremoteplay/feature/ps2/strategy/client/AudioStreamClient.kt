package com.my.psremoteplay.feature.ps2.strategy.client

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.my.psremoteplay.core.streaming.Logger
import java.net.DatagramPacket
import java.net.DatagramSocket

/**
 * Receives raw PCM audio over UDP and plays via AudioTrack.
 *
 * Expects: 48kHz, 16-bit, stereo PCM (interleaved) from the server's
 * ScreenCaptureKit audio capture.
 */
class AudioStreamClient(private val logger: Logger) {

    private val sampleRate = 48000
    private val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @Volatile private var receiving = false
    private var socket: DatagramSocket? = null
    private var receiverThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    fun start(audioPort: Int): Boolean {
        return try {
            val minBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufSize = maxOf(minBuf, 8192)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack!!.play()

            val s = DatagramSocket(audioPort)
            s.soTimeout = 5000
            s.receiveBufferSize = 256 * 1024
            socket = s
            receiving = true

            receiverThread = Thread {
                receiveLoop(s)
            }.apply { isDaemon = true; name = "audio-receiver"; start() }

            logger.log("AUDIO", "Listening on UDP port $audioPort (48kHz stereo PCM)")
            true
        } catch (e: Exception) {
            logger.error("AUDIO", "Start failed: ${e.message}", e)
            false
        }
    }

    fun stop() {
        receiving = false
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        receiverThread?.join(2000)
        receiverThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun receiveLoop(socket: DatagramSocket) {
        val buf = ByteArray(4096)
        var firstPacket = true

        try {
            while (receiving) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(packet)
                } catch (_: java.net.SocketTimeoutException) {
                    continue
                }

                if (firstPacket) {
                    logger.log("AUDIO", "First audio packet: ${packet.length} bytes")
                    firstPacket = false
                }

                // Write raw PCM to AudioTrack
                audioTrack?.write(buf, 0, packet.length)
            }
        } catch (e: Exception) {
            if (receiving) logger.error("AUDIO", "Receive error: ${e.message}")
        }
    }
}
