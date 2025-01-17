package objects

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import objects.packets.*
import objects.packets.objects.*
import java.io.*
import java.net.*

class GameConnection(
    socket: DefaultWebSocketServerSession,
) {
    private val clientSocket = socket
    private var userInfo: UserInfo? = null

    fun getUserInfo(): UserInfo = userInfo!!

    suspend fun connect() {
        println("Waiting for client info")
        val clientInfo = receivePacket()
        println("client info received")
        when (clientInfo) {
            null -> {
                close()
                throw RuntimeException("Connection was closed unexpectedly")
            }

            is ClientInfoPacket -> {
                val currentProtocolVersion = 1
                if (clientInfo.protocol_version != currentProtocolVersion) {
                    sendPacket(
                        DisconnectPacket(
                            DisconnectPacket.Reason.protocol_too_old,
                            "Protocol version ${clientInfo.protocol_version} isn't supported anymore, please update to version $currentProtocolVersion",
                        ),
                    )
                } else {
                    println(
                        "Client '${clientInfo.client_name}' v${clientInfo.client_version} connected using protocol v${clientInfo.protocol_version}",
                    )
                    sendPacket(ClientInfoAcceptPacket())
                }
            }

            else -> {
                sendPacket(UnknownPacketPacket("expected ${PacketType.CLIENT_INFO} packet"))
            }
        }

        println("waiting for auth packet")
        val authPacket = receivePacket()
        println("auth packet received")
        when (authPacket) {
            null -> {
                close()
                throw RuntimeException("Connection was closed unexpectedly")
            }

            is AuthenticatePacket -> {
                userInfo = UserInfo(authPacket.username, "somewhere, idk")
                sendPacket(AuthenticationValidPacket(false, userInfo!!))
                println("User '${authPacket.username}' has connected")
            }

            else -> {
                sendPacket(UnknownPacketPacket("expected ${PacketType.AUTHENTICATE} packet"))
            }
        }
    }

    val isOpen: Boolean
        get() = !clientSocket.outgoing.isClosedForSend

    suspend fun receivePacket(): Packet? {
        var packet: Packet? = null
        while (packet == null) {
            val text =
                try {
                    (clientSocket.incoming.receive() as Frame.Text).readText()
                } catch (e: SocketException) {
                    return null
                } catch (e: EOFException) {
                    return null
                } catch (e: ClosedReceiveChannelException) {
                    return null
                } catch (e: Exception) {
                    e.printStackTrace()
                    return null
                }

            try {
                packet = Json.decodeFromString<Packet>(text)
                println("Received '$packet'")
                when (packet) {
                    is KeepalivePacket -> {
                        packet = null
                    }
                    else -> {}
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                UnknownPacketPacket("unknown packet")
            } catch (e: SerializationException) {
                e.printStackTrace()
                UnknownPacketPacket("unknown packet")
            } catch (e: Exception) {
                e.printStackTrace()
                UnknownPacketPacket("unknown packet")
                return null
            }
        }
        return packet
    }

    suspend fun sendPacket(packet: Packet) {
        val x = Json.encodeToString(packet)
        println(x)
        clientSocket.send(x)
        clientSocket.flush()
        println("Sent '$packet'")
    }

    suspend fun close() {
        if (isOpen) {
            println("closing connection")
            clientSocket.close(CloseReason(CloseReason.Codes.NORMAL, "Bye"))
            return
        }
        println("connection was already closed")
    }
}
