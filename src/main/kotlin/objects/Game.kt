package objects

import io.ktor.websocket.*
import objects.packets.*

class Game(clientSocket: DefaultWebSocketServerSession, db: GameDatabase) {
    val connection = GameConnection(clientSocket)
    val boardManager = BoardStateManager(db, connection, null)

    val id = boardManager.gameID

    fun mainLoop() {
        val prefix = "[Game ${id}] "

        println(prefix + "Starting game")
        println(prefix + "Sending match to client")
        connection.sendPacket(MatchFoundPacket(UserInfo("Evil", "Vedals PC"), id, false))



        while (connection.isOpen) {
            when (val packet = connection.receivePacket()) {
                null -> {
                    connection.close()
                    println(prefix + "Connection was closed unexpectedly")
                }

                is GetGameStatePacket -> {
                    connection.sendPacket(UnknownPacketPacket(packet.response_id))
                    println(prefix + "GetGameState packet is not implemented yet. (response_id: ${packet.response_id}, reason: ${packet.reason}")
                }

                is AttackPacket -> {
                    boardManager.handleAttackPacket(packet, true)
                }

                is SummonPacket -> {
                    boardManager.handleSummonPacket(packet, true)
                }

                else -> {
                    connection.sendPacket(UnknownPacketPacket(null))
                    println(prefix + "Received unknown packet")
                }
            }
        }
    }
}

