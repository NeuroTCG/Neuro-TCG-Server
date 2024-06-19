package objects

import io.ktor.websocket.*
import objects.packets.*
import objects.packets.objects.*

class Game(clientSocket: DefaultWebSocketServerSession, db: GameDatabase) {
    val connection = GameConnection(clientSocket)
    val boardManager = BoardStateManager(db, connection, null)

    val id = boardManager.gameID

    suspend fun mainLoop() {
        val prefix = "[Game ${id}] "

        connection.connect()
        println(prefix + "Starting game")
        println(prefix + "Sending match to client")
        connection.sendPacket(MatchFoundPacket(UserInfo("Evil", "Vedals PC"), id, false))



        while (connection.isOpen) {
            when (val packet = connection.receivePacket()) {
                null -> {
                    connection.close()
                    println(prefix + "Connection was closed unexpectedly")
                }

                is GetBoardStatePacket -> {
                    println("getboardstate")
                    connection.sendPacket(packet.getResponsePacket(boardManager.getBoardState()))
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

