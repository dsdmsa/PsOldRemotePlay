package com.my.psremoteplay.feature.ps3.platform

import com.my.psremoteplay.core.streaming.Logger
import com.my.psremoteplay.feature.ps3.protocol.PremoConstants
import com.my.psremoteplay.feature.ps3.protocol.Ps3Discoverer
import com.my.psremoteplay.feature.ps3.protocol.Ps3Info
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.*

class AndroidPs3Discoverer(private val logger: Logger) : Ps3Discoverer {

    override suspend fun discover(timeoutMs: Int): Ps3Info? = withContext(Dispatchers.IO) {
        discoverViaBroadcast(timeoutMs)
            ?: discoverViaSubnet(timeoutMs)
    }

    override suspend fun discoverDirect(ip: String, timeoutMs: Int): Ps3Info? = withContext(Dispatchers.IO) {
        val udpResult = try {
            logger.log("DISCOVERY", "Trying UDP SRCH to $ip:${PremoConstants.PORT}...")
            sendSrchTo(InetAddress.getByName(ip), timeoutMs)
        } catch (e: Exception) {
            logger.log("DISCOVERY", "UDP SRCH failed: ${e.message}")
            null
        }
        if (udpResult != null) return@withContext udpResult

        return@withContext try {
            logger.log("DISCOVERY", "Trying TCP connection to $ip:${PremoConstants.PORT}...")
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, PremoConstants.PORT), timeoutMs)
            socket.close()
            logger.log("DISCOVERY", "TCP port ${PremoConstants.PORT} is OPEN on $ip!")
            Ps3Info(ip = ip, mac = ByteArray(6), nickname = "(TCP detected)", npxId = ByteArray(12))
        } catch (e: Exception) {
            logger.error("DISCOVERY", "TCP connection to $ip:${PremoConstants.PORT} failed: ${e.message}")
            null
        }
    }

    private fun discoverViaBroadcast(timeoutMs: Int): Ps3Info? {
        return try {
            logger.log("DISCOVERY", "Trying broadcast on port ${PremoConstants.PORT}...")
            val socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = timeoutMs

            val srch = PremoConstants.SRCH_PACKET
            socket.send(DatagramPacket(srch, srch.size, InetAddress.getByName("255.255.255.255"), PremoConstants.PORT))

            val recvBuf = ByteArray(256)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)
            socket.close()

            parseResponse(recvPacket)
        } catch (_: SocketTimeoutException) {
            logger.log("DISCOVERY", "Broadcast timed out")
            null
        } catch (e: Exception) {
            logger.error("DISCOVERY", "Broadcast error: ${e.message}")
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
                    if (addr.address is Inet4Address) {
                        val result = sendSrchTo(broadcast, timeoutMs)
                        if (result != null) return result
                    }
                }
            }
            null
        } catch (e: Exception) {
            logger.error("DISCOVERY", "Subnet scan error: ${e.message}")
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
            parseResponse(recvPacket)
        } catch (_: SocketTimeoutException) { null }
        catch (e: Exception) { null }
        finally { socket.close() }
    }

    private fun parseResponse(packet: DatagramPacket): Ps3Info? {
        val data = packet.data
        if (packet.length < 156) return null
        val magic = String(data, 0, 4, Charsets.US_ASCII)
        if (magic != "RESP") return null

        val mac = data.copyOfRange(10, 16)
        val nickname = String(data, 16, 128, Charsets.US_ASCII).trimEnd('\u0000')
        val npxId = data.copyOfRange(144, 156)

        val info = Ps3Info(ip = packet.address.hostAddress ?: "", mac = mac, nickname = nickname, npxId = npxId)
        logger.log("DISCOVERY", "PS3 found: ${info.nickname} at ${info.ip}")
        return info
    }
}
