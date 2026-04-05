package com.my.psremoteplay.feature.ps2.strategy.client

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import androidx.compose.ui.graphics.ImageBitmap
import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.protocol.StreamStats
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * High-performance Android H.264 client using MediaCodec hardware decoder.
 *
 * Pipeline: UDP receive (raw H.264 Annex B) → accumulate → split NAL units →
 *           MediaCodec HW decode → Surface (zero-copy).
 *
 * The server (FfmpegH264UdpServer) sends raw H.264 directly via UDP.
 * This client accumulates bytes, splits on 00 00 00 01 start codes,
 * identifies SPS/PPS for decoder config, and feeds NAL units to MediaCodec.
 *
 * Frame drop policy: always render the newest frame, drop older queued frames.
 */
class AndroidMediaCodecClient(private val logger: Logger) : VideoStreamClient {
    override val name = "H.264/MediaCodec"
    override val usesSurfaceRendering = true

    override val currentFrame: StateFlow<ImageBitmap?> = MutableStateFlow(null)
    override val decodedFrameCount: Long get() = framesDecoded.get()

    @Volatile private var receiving = false
    private var socket: DatagramSocket? = null
    private var receiverThread: Thread? = null
    private var decoderThread: Thread? = null
    private var decoder: MediaCodec? = null
    @Volatile private var surface: Surface? = null

    private val framesReceived = AtomicLong(0)
    private val framesDecoded = AtomicLong(0)
    private val framesDropped = AtomicLong(0)
    private val totalDecodeNs = AtomicLong(0)
    private val statsStartTime = AtomicLong(0)

    private val nalQueue = ConcurrentLinkedQueue<ByteArray>()

    @Volatile private var spsData: ByteArray? = null
    @Volatile private var ppsData: ByteArray? = null
    @Volatile private var decoderConfigured = false
    @Volatile private var configWidth = 640
    @Volatile private var configHeight = 448

    fun setSurface(s: Surface?) {
        surface = s
        if (s != null && spsData != null && ppsData != null && !decoderConfigured) {
            configureDecoder(spsData!!, ppsData!!, s)
        }
    }

    fun getStats(): StreamStats {
        val decoded = framesDecoded.get()
        val avgDecode = if (decoded > 0) totalDecodeNs.get() / decoded / 1_000_000 else 0
        val elapsed = System.currentTimeMillis() - statsStartTime.get()
        val fps = if (elapsed > 0) (decoded * 1000 / maxOf(elapsed, 1)).toInt() else 0
        return StreamStats(
            framesReceived = framesReceived.get(),
            framesDecoded = decoded,
            framesDropped = framesDropped.get(),
            avgDecodeMs = avgDecode,
            currentFps = fps
        )
    }

    override fun start(serverIp: String, config: StreamConfig): Boolean {
        return try {
            configWidth = config.width
            configHeight = config.height

            val s = DatagramSocket(config.videoPort)
            s.soTimeout = 5000
            s.receiveBufferSize = 2 * 1024 * 1024
            socket = s
            receiving = true
            statsStartTime.set(System.currentTimeMillis())

            receiverThread = Thread {
                receiveLoop(s)
            }.apply { isDaemon = true; name = "h264-receiver"; start() }

            decoderThread = Thread {
                decodeLoop()
            }.apply { isDaemon = true; name = "mediacodec-decoder"; start() }

            logger.log(name, "Listening for raw H.264/UDP on port ${config.videoPort}")
            true
        } catch (e: Exception) {
            logger.error(name, "Start failed: ${e.message}", e)
            false
        }
    }

    override fun stop() {
        receiving = false
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        receiverThread?.join(2000)
        decoderThread?.join(2000)
        receiverThread = null
        decoderThread = null

        try {
            decoder?.stop()
            decoder?.release()
        } catch (_: Exception) {}
        decoder = null
        decoderConfigured = false
        spsData = null
        ppsData = null
        nalQueue.clear()

        framesReceived.set(0)
        framesDecoded.set(0)
        framesDropped.set(0)
        totalDecodeNs.set(0)
    }

    override fun isReceiving() = receiving

    /**
     * Receives raw H.264 Annex B bytes from UDP datagrams.
     * Accumulates in a buffer, splits NAL units on 00 00 00 01 start codes,
     * identifies SPS/PPS, and enqueues for decoding.
     */
    private fun receiveLoop(socket: DatagramSocket) {
        val buf = ByteArray(65536)
        val accumulator = NalAccumulator()
        var firstPacketLogged = false
        var totalBytes = 0L

        try {
            while (receiving) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(packet)
                } catch (_: java.net.SocketTimeoutException) {
                    if (!firstPacketLogged) logger.log(name, "Waiting for H.264 data on UDP...")
                    continue
                }

                if (!firstPacketLogged) {
                    logger.log(name, "First UDP packet: ${packet.length} bytes")
                    firstPacketLogged = true
                }

                totalBytes += packet.length
                accumulator.feed(buf, 0, packet.length)

                // Extract complete NAL units
                while (true) {
                    val nalUnit = accumulator.poll() ?: break
                    if (nalUnit.size < 5) continue

                    framesReceived.incrementAndGet()
                    val nalType = getNalType(nalUnit)

                    when (nalType) {
                        NAL_TYPE_SPS -> {
                            logger.log(name, "SPS received (${nalUnit.size} bytes)")
                            spsData = nalUnit.clone()
                            tryConfigureDecoder()
                        }
                        NAL_TYPE_PPS -> {
                            logger.log(name, "PPS received (${nalUnit.size} bytes)")
                            ppsData = nalUnit.clone()
                            tryConfigureDecoder()
                        }
                        else -> {
                            if (!decoderConfigured) {
                                framesDropped.incrementAndGet()
                                continue
                            }
                            // Drop non-IDR if queue is backing up
                            if (nalQueue.size > 3 && nalType != NAL_TYPE_IDR) {
                                framesDropped.incrementAndGet()
                                continue
                            }
                            nalQueue.offer(nalUnit)
                        }
                    }
                }

                // Log periodically
                if (totalBytes % (1024 * 1024) < packet.length.toLong()) {
                    logger.log(name, "Received ${totalBytes / 1024}KB, NALs queued=${nalQueue.size}, decoded=${framesDecoded.get()}")
                }
            }
        } catch (e: Exception) {
            if (receiving) logger.error(name, "Receive error: ${e.message}")
        }
    }

    private fun decodeLoop() {
        try {
            while (receiving) {
                val nalUnit = nalQueue.poll()
                if (nalUnit == null) {
                    Thread.sleep(1)
                    continue
                }

                val codec = decoder ?: continue
                feedNalToDecoder(codec, nalUnit)
                drainDecoder(codec)
            }
        } catch (e: Exception) {
            if (receiving) logger.error(name, "Decode error: ${e.message}", e)
        }
    }

    private fun feedNalToDecoder(codec: MediaCodec, nalData: ByteArray) {
        val inputIndex = codec.dequeueInputBuffer(10_000)
        if (inputIndex < 0) return

        val inputBuffer = codec.getInputBuffer(inputIndex) ?: return
        inputBuffer.clear()
        inputBuffer.put(nalData)
        codec.queueInputBuffer(inputIndex, 0, nalData.size, System.nanoTime() / 1000, 0)
    }

    private fun drainDecoder(codec: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        var lastOutputIndex = -1

        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(info, 0)
            if (outputIndex < 0) break

            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                codec.releaseOutputBuffer(outputIndex, false)
                continue
            }

            if (lastOutputIndex >= 0) {
                codec.releaseOutputBuffer(lastOutputIndex, false)
                framesDropped.incrementAndGet()
            }
            lastOutputIndex = outputIndex
        }

        if (lastOutputIndex >= 0) {
            val t0 = System.nanoTime()
            codec.releaseOutputBuffer(lastOutputIndex, true)
            totalDecodeNs.addAndGet(System.nanoTime() - t0)
            val decoded = framesDecoded.incrementAndGet()

            if (decoded % 150 == 0L) {
                val stats = getStats()
                logger.log("PERF",
                    "decode=${stats.avgDecodeMs}ms fps=${stats.currentFps} " +
                    "decoded=$decoded dropped=${stats.framesDropped}")
            }
        }
    }

    private fun tryConfigureDecoder() {
        val sps = spsData ?: return
        val pps = ppsData ?: return
        val s = surface ?: return
        if (decoderConfigured) return
        configureDecoder(sps, pps, s)
    }

    private fun configureDecoder(sps: ByteArray, pps: ByteArray, surface: Surface) {
        try {
            val spsPayload = stripStartCode(sps)
            val ppsPayload = stripStartCode(pps)

            val format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, configWidth, configHeight
            )
            format.setByteBuffer("csd-0", ByteBuffer.wrap(byteArrayOf(0, 0, 0, 1) + spsPayload))
            format.setByteBuffer("csd-1", ByteBuffer.wrap(byteArrayOf(0, 0, 0, 1) + ppsPayload))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                format.setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE.toInt())
                format.setInteger(MediaFormat.KEY_PRIORITY, 0)
            }

            for (key in VENDOR_LOW_LATENCY_KEYS) {
                try { format.setInteger(key, 1) } catch (_: Exception) {}
            }

            val codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, surface, null, 0)
            codec.start()

            decoder = codec
            decoderConfigured = true
            logger.log(name, "MediaCodec configured: ${codec.name}")
        } catch (e: Exception) {
            logger.error(name, "MediaCodec configure failed: ${e.message}", e)
        }
    }

    private fun getNalType(nalUnit: ByteArray): Int {
        val offset = findNalTypeOffset(nalUnit)
        return if (offset >= 0 && offset < nalUnit.size) nalUnit[offset].toInt() and 0x1F else -1
    }

    private fun findNalTypeOffset(data: ByteArray): Int {
        if (data.size >= 4 && data[0] == 0.toByte() && data[1] == 0.toByte() &&
            data[2] == 0.toByte() && data[3] == 1.toByte()
        ) return 4
        if (data.size >= 3 && data[0] == 0.toByte() && data[1] == 0.toByte() &&
            data[2] == 1.toByte()
        ) return 3
        return 0
    }

    private fun stripStartCode(nalUnit: ByteArray): ByteArray {
        if (nalUnit.size >= 4 && nalUnit[0] == 0.toByte() && nalUnit[1] == 0.toByte() &&
            nalUnit[2] == 0.toByte() && nalUnit[3] == 1.toByte()
        ) return nalUnit.copyOfRange(4, nalUnit.size)
        if (nalUnit.size >= 3 && nalUnit[0] == 0.toByte() && nalUnit[1] == 0.toByte() &&
            nalUnit[2] == 1.toByte()
        ) return nalUnit.copyOfRange(3, nalUnit.size)
        return nalUnit
    }

    companion object {
        private const val NAL_TYPE_IDR = 5
        private const val NAL_TYPE_SPS = 7
        private const val NAL_TYPE_PPS = 8

        private val VENDOR_LOW_LATENCY_KEYS = listOf(
            "vendor.low-latency.enable",
            "vendor.hisi-video-dec-low-latency-mode",
            "vendor.rtc-ext-dec-low-latency.enable",
            "vdec-lowlatency"
        )
    }
}

/**
 * Accumulates raw H.264 Annex B bytes and splits on 00 00 00 01 start codes.
 * Each poll() returns one complete NAL unit including its start code.
 */
private class NalAccumulator {
    private var buffer = ByteArray(262144)
    private var writePos = 0
    private var scanPos = 4

    fun feed(data: ByteArray, offset: Int, length: Int) {
        ensureCapacity(writePos + length)
        data.copyInto(buffer, writePos, offset, offset + length)
        writePos += length
    }

    fun poll(): ByteArray? {
        var i = scanPos
        while (i + 3 < writePos) {
            if (buffer[i] == 0.toByte() && buffer[i + 1] == 0.toByte() &&
                buffer[i + 2] == 0.toByte() && buffer[i + 3] == 1.toByte()
            ) {
                if (i > 0) {
                    val nalUnit = buffer.copyOfRange(0, i)
                    val remaining = writePos - i
                    buffer.copyInto(buffer, 0, i, writePos)
                    writePos = remaining
                    scanPos = 4
                    return nalUnit
                }
            }
            i++
        }
        scanPos = maxOf(4, i)
        return null
    }

    private fun ensureCapacity(needed: Int) {
        if (needed <= buffer.size) return
        var newSize = buffer.size
        while (newSize < needed) newSize *= 2
        val newBuf = ByteArray(newSize)
        buffer.copyInto(newBuf, 0, 0, writePos)
        buffer = newBuf
    }
}
