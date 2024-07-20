package objects

import objects.packets.*

class Game(val p1Connection: GameConnection, val p2connection: GameConnection, db: GameDatabase) {
    val boardManager = BoardStateManager(db, p1Connection, p2connection)

    val id = boardManager.gameID

    suspend fun mainLoop(firstPlayer: Boolean) {
        val prefix = "[Game ${id}][Player ${if (firstPlayer) 1 else 2}] "
        val connection = if (firstPlayer) p1Connection else p2connection
        val otherConnection = if (firstPlayer) p2connection else p1Connection

        println(prefix + "Starting game")
        println(prefix + "Sending game rules to client")
        connection.sendPacket(RuleInfoPacket())
        println(prefix + "Sending match to client")
        connection.sendPacket(MatchFoundPacket(otherConnection.getUserInfo(), id, false, firstPlayer))

        if (firstPlayer)
            connection.sendPacket(StartTurnPacket())

        while (connection.isOpen) {
            when (val packet = connection.receivePacket()) {
                null -> {
                    if (connection.isOpen)
                        connection.close()
                    println(prefix + "Connection was closed unexpectedly")

                    if (otherConnection.isOpen) {
                        println(prefix + "Informing opponent")
                        otherConnection.sendPacket(
                            DisconnectPacket(
                                DisconnectPacket.Reason.opponent_disconnect,
                                "The opponent has closed it's connection"
                            )
                        )
                        //otherConnection.close()
                    }
                }

                is GetBoardStatePacket -> {
                    println(prefix + "getboardstate")
                    connection.sendPacket(GetBoardStateResponse(boardManager.getBoardState()))
                }

                is AttackRequestPacket -> {
                    boardManager.handleAttackPacket(packet, firstPlayer)
                }

                is SummonRequestPacket -> {
                    boardManager.handleSummonPacket(packet, firstPlayer)
                }

                is SwitchPlaceRequestPacket ->{
                    boardManager.handleSwitchPlacePacket(packet, firstPlayer)
                }

                is EndTurnPacket -> {
                    boardManager.handleEndTurn(firstPlayer)
                }

                is DrawCardRequestPacket -> {
                    boardManager.handleDrawCard(firstPlayer)
                }

                else -> {
                    connection.sendPacket(UnknownPacketPacket("unknown packet type received"))
                    println(prefix + "Received unknown packet")
                }
            }
        }
    }
}

