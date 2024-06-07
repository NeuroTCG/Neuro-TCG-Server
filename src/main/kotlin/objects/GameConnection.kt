package objects

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import objects.packets.*
import java.io.*
import java.net.*

class GameConnection(socket: DefaultWebSocketServerSession) {
    private val clientSocket = socket

    init {
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
                            "Protocol version ${clientInfo.protocol_version} isn't supported anymore, please update to version ${currentProtocolVersion}"
                        )
                    )
                } else {
                    println("Client '${clientInfo.client_name}' v${clientInfo.client_version} connected using protocol v${clientInfo.protocol_version}")
                    sendPacket(ClientInfoAcceptPacket())
                }
            }

            else -> {
                sendPacket(UnknownPacketPacket(null))
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
                sendPacket(AuthenticationValidPacket(false))
                println("User '${authPacket.username}' has connected")
            }

            else -> {
                sendPacket(UnknownPacketPacket(null))
            }
        }
    }

    var isOpen = true
        private set

    fun receivePacket(): Packet? {
        return try {
            val packet =
                runBlocking { Json.decodeFromString<Packet>((clientSocket.incoming.receive() as Frame.Text).readText()) }
            println("Received '${packet}'")
            packet
        } catch (e: SocketException) {
            null
        } catch (e: EOFException) {
            null
        } catch (e: ClosedReceiveChannelException) {
            null
        }
    }

    fun sendPacket(packet: Packet) {
        runBlocking {
            clientSocket.send(Json.encodeToString(packet))
            clientSocket.flush()
            println("Sent '${packet}'")
        }
    }

    fun close() {
        runBlocking { clientSocket.close(CloseReason(CloseReason.Codes.NORMAL, "Bye")) }
        isOpen = false
    }
}
