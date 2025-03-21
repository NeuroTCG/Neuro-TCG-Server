package objects

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.delay
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import objects.packets.*
import objects.packets.objects.*
import java.io.*
import java.net.*

class GameConnection(
    socket: DefaultWebSocketServerSession,
    val db: GameDatabase,
) {
    private val clientSocket = socket
    private var userInfo: UserInfo? = null

    fun getUserInfo(): UserInfo = userInfo!!

    suspend fun connectAndAuthenticate(queue: MatchmakingQueue) {
        println("Waiting for client info")
        val clientInfo = receivePacket()
        println("client info received")
        when (clientInfo) {
            null -> {
                close()
                return
            }

            is ClientInfoPacket -> {
                val currentProtocolVersion = 1
                if (clientInfo.protocol_version != currentProtocolVersion) {
                    sendPacket(
                        DisconnectPacket(
                            DisconnectPacket.Reason.protocol_too_old,
                            "Protocol version ${clientInfo.protocol_version} isn't supported anymore, " +
                                "please update to version $currentProtocolVersion",
                        ),
                    )
                    close()
                    return
                } else {
                    println(
                        "Client '${clientInfo.client_name}' v${clientInfo.client_version} connected " +
                            "using protocol v${clientInfo.protocol_version}",
                    )
                    sendPacket(ClientInfoAcceptPacket())
                }
            }

            else -> {
                sendPacket(UnknownPacketPacket("expected ${PacketType.CLIENT_INFO} packet"))
                close()
                return
            }
        }

        println("waiting for auth packet")
        val authPacket = receivePacket()
        println("auth packet received")
        when (authPacket) {
            null -> {
                close()
                return
            }

            is AuthenticatePacket -> {
                if (db.checkToken(authPacket.token)) {
                    val userId = db.getUserIdFromToken(authPacket.token)!!
                    userInfo = UserInfo(userId)
                    sendPacket(AuthenticationValidPacket(false, userInfo!!))
                    println("User '$userId' has connected")
                } else {
                    sendPacket(DisconnectPacket(DisconnectPacket.Reason.auth_invalid, "Token is invalid"))
                    close()
                    return
                }
            }

            else -> {
                sendPacket(UnknownPacketPacket("expected ${PacketType.AUTHENTICATE} packet"))
                close()
                return
            }
        }
    }

    val isOpen: Boolean
        get() = !clientSocket.outgoing.isClosedForSend && !isClosing

    private var isClosing = false
    private var closingJob: Job? = null

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
            println("closing connection in background")
            isClosing = true
            clientSocket.flush()

            closingJob =
                CoroutineScope(Dispatchers.Default).launch {
                    delay(5000) // the last message won't get sent unless we wait
                    clientSocket.close(CloseReason(CloseReason.Codes.NORMAL, "Bye"))
                    println("Backgrounded closing task finished")
                }
            return
        }
        println("connection was already closed")
    }

    suspend fun waitForClose() {
        close()
        println("Waiting for connection close")
        closingJob!!.join()
        println("Waiting for connection close finished")
    }
}
