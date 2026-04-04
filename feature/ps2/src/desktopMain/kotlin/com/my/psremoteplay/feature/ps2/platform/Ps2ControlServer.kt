package com.my.psremoteplay.feature.ps2.platform

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.feature.ps2.protocol.Ps2Protocol
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

/**
 * TCP control server for non-video data: server info, controller input.
 * Video is handled separately via RTP/UDP (JavaCvVideoStreamer).
 */
class Ps2ControlServer(private val logger: Logger) {
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    @Volatile private var running = false
    private val clients = CopyOnWriteArrayList<ClientConnection>()
    private var inputHandler: ((ControllerState) -> Unit)? = null

    private class ClientConnection(val socket: Socket, val output: OutputStream)

    fun start(port: Int) {
        if (running) return
        running = true

        try {
            serverSocket = ServerSocket(port)
            logger.log("CONTROL", "Control server listening on port $port")

            acceptThread = Thread {
                while (running) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        logger.log("CONTROL", "Client connected: ${clientSocket.remoteSocketAddress}")
                        val connection = ClientConnection(clientSocket, clientSocket.getOutputStream())
                        clients.add(connection)

                        // Send server info
                        val info = """{"game":"PS2 Game","width":640,"height":448}""".toByteArray()
                        val frame = Ps2Protocol.buildFrame(Ps2Protocol.SERVER_INFO, info)
                        connection.output.write(frame)
                        connection.output.flush()

                        Thread {
                            readClientInput(connection)
                        }.apply { isDaemon = true; start() }
                    } catch (e: Exception) {
                        if (running) logger.error("CONTROL", "Accept error: ${e.message}")
                    }
                }
            }.apply { isDaemon = true; start() }
        } catch (e: Exception) {
            logger.error("CONTROL", "Failed to start control server: ${e.message}", e)
            running = false
        }
    }

    fun onClientInput(handler: (ControllerState) -> Unit) {
        inputHandler = handler
    }

    fun getClientCount(): Int = clients.size

    fun stop() {
        running = false
        for (client in clients) {
            try { client.socket.close() } catch (_: Exception) {}
        }
        clients.clear()
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        acceptThread = null
        logger.log("CONTROL", "Control server stopped")
    }

    fun isRunning(): Boolean = running

    private fun readClientInput(connection: ClientConnection) {
        try {
            val input = connection.socket.getInputStream()
            val headerBuf = ByteArray(5)

            while (running && !connection.socket.isClosed) {
                var offset = 0
                while (offset < 5) {
                    val n = input.read(headerBuf, offset, 5 - offset)
                    if (n <= 0) return
                    offset += n
                }

                val payloadLen = ((headerBuf[0].toInt() and 0xFF) shl 24) or
                    ((headerBuf[1].toInt() and 0xFF) shl 16) or
                    ((headerBuf[2].toInt() and 0xFF) shl 8) or
                    (headerBuf[3].toInt() and 0xFF)
                val type = headerBuf[4]

                val dataLen = payloadLen - 1
                if (dataLen <= 0 || dataLen > 65536) continue

                val payload = ByteArray(dataLen)
                offset = 0
                while (offset < dataLen) {
                    val n = input.read(payload, offset, dataLen - offset)
                    if (n <= 0) return
                    offset += n
                }

                when (type) {
                    Ps2Protocol.CLIENT_HELLO -> {
                        logger.log("CONTROL", "Client hello: ${String(payload)}")
                    }
                    Ps2Protocol.CONTROLLER_STATE -> {
                        if (payload.size >= Ps2Protocol.CONTROLLER_PAYLOAD_SIZE) {
                            val state = Ps2Protocol.decodeControllerState(payload)
                            inputHandler?.invoke(state)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (running) logger.log("CONTROL", "Client disconnected: ${e.message}")
        } finally {
            removeClient(connection)
        }
    }

    private fun removeClient(connection: ClientConnection) {
        clients.remove(connection)
        try { connection.socket.close() } catch (_: Exception) {}
        logger.log("CONTROL", "Client removed (${clients.size} remaining)")
    }
}
