package com.my.psoldremoteplay

import com.my.psoldremoteplay.protocol.Ps3Discoverer
import com.my.psoldremoteplay.protocol.Ps3Info
import com.my.psoldremoteplay.protocol.PremoConstants
import com.my.psoldremoteplay.protocol.PremoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.*

class JvmPs3Discoverer(private val logger: PremoLogger) : Ps3Discoverer {

    override suspend fun discover(timeoutMs: Int): Ps3Info? = withContext(Dispatchers.IO) {
        // Try broadcast first, then directed if we have a subnet
        discoverViaBroadcast(timeoutMs)
            ?: discoverViaSubnet(timeoutMs)
    }

    private fun discoverViaBroadcast(timeoutMs: Int): Ps3Info? {
        return try {
            logger.log("DISCOVERY", "Trying 255.255.255.255 broadcast on port ${PremoConstants.PORT}...")
            val socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = timeoutMs

            val srch = PremoConstants.SRCH_PACKET
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val sendPacket = DatagramPacket(srch, srch.size, broadcastAddr, PremoConstants.PORT)
            logger.log("DISCOVERY", "Sending SRCH packet (${srch.size} bytes) to 255.255.255.255:${PremoConstants.PORT}")
            socket.send(sendPacket)

            logger.log("DISCOVERY", "Waiting for RESP (timeout: ${timeoutMs}ms)...")
            val recvBuf = ByteArray(256)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)
            socket.close()

            logger.log("DISCOVERY", "Received ${recvPacket.length} bytes from ${recvPacket.address.hostAddress}")
            parseResponse(recvPacket)
        } catch (e: SocketTimeoutException) {
            logger.log("DISCOVERY", "Broadcast timed out after ${timeoutMs}ms — no PS3 responded")
            null
        } catch (e: Exception) {
            logger.error("DISCOVERY", "Broadcast error: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    private fun discoverViaSubnet(timeoutMs: Int): Ps3Info? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            for (iface in interfaces) {
                if (iface.isLoopback || !iface.isUp) continue
                for (addr in iface.interfaceAddresses) {
                    val broadcast = addr.broadcast ?: continue
                    val ip = addr.address
                    if (ip is Inet4Address) {
                        logger.log("DISCOVERY", "Trying subnet broadcast ${broadcast.hostAddress} via ${iface.displayName} (${ip.hostAddress}/${addr.networkPrefixLength})")
                        val result = sendSrchTo(broadcast, timeoutMs)
                        if (result != null) return result
                    }
                }
            }
            logger.log("DISCOVERY", "No PS3 found on any subnet")
            null
        } catch (e: Exception) {
            logger.error("DISCOVERY", "Subnet scan error: ${e.message}")
            null
        }
    }

    override suspend fun discoverDirect(ip: String, timeoutMs: Int): Ps3Info? = withContext(Dispatchers.IO) {
        // Try UDP SRCH first
        val udpResult = try {
            logger.log("DISCOVERY", "Trying UDP SRCH to $ip:${PremoConstants.PORT}...")
            sendSrchTo(InetAddress.getByName(ip), timeoutMs)
        } catch (e: Exception) {
            logger.log("DISCOVERY", "UDP SRCH failed: ${e.message}")
            null
        }
        if (udpResult != null) return@withContext udpResult

        // Fallback: try TCP connection to port 9293
        return@withContext try {
            logger.log("DISCOVERY", "Trying TCP connection to $ip:${PremoConstants.PORT}...")
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(ip, PremoConstants.PORT), timeoutMs)
            socket.close()
            logger.log("DISCOVERY", "TCP port ${PremoConstants.PORT} is OPEN on $ip!")
            logger.log("DISCOVERY", "PS3 is listening (TCP). UDP discovery may be disabled but protocol works.")

            // Return a minimal Ps3Info — we know the IP but not MAC/nickname yet
            Ps3Info(
                ip = ip,
                mac = ByteArray(6),
                nickname = "(TCP detected — details pending)",
                npxId = ByteArray(12)
            )
        } catch (e: Exception) {
            logger.error("DISCOVERY", "TCP connection to $ip:${PremoConstants.PORT} also failed: ${e.message}")
            logger.log("DISCOVERY", "PS3 is not listening on port ${PremoConstants.PORT}")
            null
        }
    }

    private fun sendSrchTo(addr: InetAddress, timeoutMs: Int): Ps3Info? {
        val socket = DatagramSocket()
        socket.soTimeout = timeoutMs
        return try {
            val srch = PremoConstants.SRCH_PACKET
            socket.send(DatagramPacket(srch, srch.size, addr, PremoConstants.PORT))

            val recvBuf = ByteArray(256)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)

            logger.log("DISCOVERY", "Got ${recvPacket.length} bytes from ${recvPacket.address.hostAddress}")
            parseResponse(recvPacket)
        } catch (_: SocketTimeoutException) {
            null
        } catch (e: Exception) {
            logger.error("DISCOVERY", "Send to ${addr.hostAddress} failed: ${e.message}")
            null
        } finally {
            socket.close()
        }
    }

    private fun parseResponse(packet: DatagramPacket): Ps3Info? {
        val data = packet.data
        if (packet.length < 156) {
            logger.error("DISCOVERY", "Response too short: ${packet.length} bytes (expected 156)")
            return null
        }

        val magic = String(data, 0, 4, Charsets.US_ASCII)
        if (magic != "RESP") {
            logger.error("DISCOVERY", "Invalid magic: '$magic' (expected 'RESP')")
            logger.log("DISCOVERY", "First 16 bytes: ${data.take(16).joinToString(" ") { "%02X".format(it) }}")
            return null
        }

        val mac = data.copyOfRange(10, 16)
        val nickname = String(data, 16, 128, Charsets.US_ASCII).trimEnd('\u0000')
        val npxId = data.copyOfRange(144, 156)

        val info = Ps3Info(
            ip = packet.address.hostAddress ?: "",
            mac = mac,
            nickname = nickname,
            npxId = npxId
        )

        logger.log("DISCOVERY", "PS3 found!")
        logger.log("DISCOVERY", "  Nickname: ${info.nickname}")
        logger.log("DISCOVERY", "  IP: ${info.ip}")
        logger.log("DISCOVERY", "  MAC: ${info.macString}")
        logger.log("DISCOVERY", "  NPX ID: ${npxId.joinToString("") { "%02X".format(it) }}")
        return info
    }
}
