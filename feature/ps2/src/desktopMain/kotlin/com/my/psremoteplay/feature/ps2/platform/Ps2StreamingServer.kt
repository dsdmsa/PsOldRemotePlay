package com.my.psremoteplay.feature.ps2.platform

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.core.streaming.input.ControllerState
import com.my.psremoteplay.feature.ps2.protocol.Ps2Protocol
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

class Ps2StreamingServer(private val logger: Logger) {
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    @Volatile private var running = false
    private val clients = CopyOnWriteArrayList<ClientConnection>()
    private var inputHandler: ((ControllerState) -> Unit)? = null

    private class ClientConnection(val socket: Socket, val output: OutputStream)

    fun start(port: Int = Ps2Protocol.DEFAULT_PORT) {
        if (running) return
        running = true

        try {
            serverSocket = ServerSocket(port)
            logger.log("SERVER", "Listening on port $port")

            acceptThread = Thread {
                while (running) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        logger.log("SERVER", "Client connected: ${clientSocket.remoteSocketAddress}")
                        val connection = ClientConnection(clientSocket, clientSocket.getOutputStream())
                        clients.add(connection)

                        // Send server info
                        val info = """{"game":"PS2 Game","width":640,"height":448}""".toByteArray()
                        val frame = Ps2Protocol.buildFrame(Ps2Protocol.SERVER_INFO, info)
                        connection.output.write(frame)
                        connection.output.flush()

                        // Start reading client input in a separate thread
                        Thread {
                            readClientInput(connection)
                        }.apply { isDaemon = true; start() }
                    } catch (e: Exception) {
                        if (running) logger.error("SERVER", "Accept error: ${e.message}")
                    }
                }
            }.apply { isDaemon = true; start() }
        } catch (e: Exception) {
            logger.error("SERVER", "Failed to start server: ${e.message}", e)
            running = false
        }
    }

    fun broadcastVideoFrame(nalUnit: ByteArray) {
        if (clients.isEmpty()) return
        val frame = Ps2Protocol.buildFrame(Ps2Protocol.VIDEO_FRAME, nalUnit)
        val disconnected = mutableListOf<ClientConnection>()

        for (client in clients) {
            try {
                client.output.write(frame)
                client.output.flush()
            } catch (e: Exception) {
                disconnected.add(client)
            }
        }

        for (client in disconnected) {
            removeClient(client)
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
        logger.log("SERVER", "Server stopped")
    }

    fun isRunning(): Boolean = running

    private fun readClientInput(connection: ClientConnection) {
        try {
            val input = connection.socket.getInputStream()
            val headerBuf = ByteArray(5) // 4 bytes length + 1 byte type

            while (running && !connection.socket.isClosed) {
                // Read frame header (4-byte length + 1-byte type)
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

                // Read payload (length includes the type byte, so payload = length - 1)
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
                        logger.log("SERVER", "Client hello: ${String(payload)}")
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
            if (running) logger.log("SERVER", "Client disconnected: ${e.message}")
        } finally {
            removeClient(connection)
        }
    }

    private fun removeClient(connection: ClientConnection) {
        clients.remove(connection)
        try { connection.socket.close() } catch (_: Exception) {}
        logger.log("SERVER", "Client removed (${clients.size} remaining)")
    }
}
