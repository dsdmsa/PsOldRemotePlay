package com.my.psremoteplay.feature.ps2.strategy.server

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps2.strategy.StreamConfig
import com.my.psremoteplay.feature.ps2.strategy.VideoStreamServer
import java.io.File
import java.io.FileInputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Reads video from PCSX2's built-in capture output (named pipe or file).
 * PCSX2 must be configured to capture video to a named FIFO.
 *
 * Setup:
 *   mkfifo /tmp/pcsx2_video
 *   In PCSX2 settings: enable video capture, set output to /tmp/pcsx2_video,
 *   codec to "mjpeg" or "rawvideo", container to "nut" or "avi".
 *
 * This strategy reads JPEG frames from the pipe and forwards via UDP.
 * Eliminates screen capture entirely — lowest possible capture latency.
 */
class Pcsx2PipeServer(
    private val logger: Logger,
    private val pipePath: String = "/tmp/pcsx2_video"
) : VideoStreamServer {
    override val name = "PCSX2-Pipe/UDP"

    @Volatile private var streaming = false
    private var readThread: Thread? = null
    private var socket: DatagramSocket? = null

    override fun start(config: StreamConfig) {
        if (streaming) return

        val pipeFile = File(pipePath)
        if (!pipeFile.exists()) {
            logger.error(name, "Pipe not found: $pipePath. Create it with: mkfifo $pipePath")
            logger.log(name, "Then configure PCSX2 to capture video to $pipePath")
            return
        }

        streaming = true
        socket = DatagramSocket()
        socket?.broadcast = true

        readThread = Thread {
            try {
                val udpSocket = socket!!
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val buffer = ByteArray(65536)

                logger.log(name, "Reading from PCSX2 pipe: $pipePath")
                val input = FileInputStream(pipeFile)

                // Read MJPEG stream: find SOI (0xFF 0xD8) and EOI (0xFF 0xD9) markers
                val accumulator = java.io.ByteArrayOutputStream(131072)
                var inFrame = false

                while (streaming) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead <= 0) {
                        Thread.sleep(1)
                        continue
                    }

                    for (i in 0 until bytesRead) {
                        if (!inFrame) {
                            if (accumulator.size() == 1 && buffer[i] == 0xD8.toByte()) {
                                accumulator.write(buffer[i].toInt())
                                inFrame = true
                            } else if (buffer[i] == 0xFF.toByte()) {
                                accumulator.reset()
                                accumulator.write(0xFF)
                            } else {
                                accumulator.reset()
                            }
                        } else {
                            accumulator.write(buffer[i].toInt())
                            val size = accumulator.size()
                            if (size >= 4) {
                                val data = accumulator.toByteArray()
                                if (data[size - 2] == 0xFF.toByte() && data[size - 1] == 0xD9.toByte()) {
                                    // Complete JPEG frame — send via UDP
                                    val packet = DatagramPacket(data, data.size, broadcastAddr, config.videoPort)
                                    try { udpSocket.send(packet) } catch (_: Exception) {}
                                    accumulator.reset()
                                    inFrame = false
                                }
                            }
                        }
                    }

                    if (accumulator.size() > 2_000_000) {
                        accumulator.reset()
                        inFrame = false
                    }
                }

                input.close()
            } catch (e: Exception) {
                if (streaming) logger.error(name, "Pipe read error: ${e.message}", e)
            }
        }.apply { isDaemon = true; name = "pcsx2-pipe-reader"; start() }
    }

    override fun stop() {
        streaming = false
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        readThread?.join(2000)
        readThread = null
        logger.log(name, "Stopped")
    }

    override fun isStreaming() = streaming
}
