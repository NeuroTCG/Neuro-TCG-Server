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
        println(prefix + "Sending game rules to client")
        connection.sendPacket(RuleInfoPacket())
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
                    connection.sendPacket(GetBoardStateResponse(boardManager.getBoardState()))
                }

                is AttackRequestPacket -> {
                    boardManager.handleAttackPacket(packet, true)
                }

                is SummonRequestPacket -> {
                    boardManager.handleSummonPacket(packet, true)
                }

                else -> {
                    connection.sendPacket(UnknownPacketPacket("unknown packet type received"))
                    println(prefix + "Received unknown packet")
                }
            }
        }
    }
}

