package objects

import objects.packets.*

class Game(
    val p1Connection: GameConnection,
    val p2connection: GameConnection,
    db: GameDatabase,
) {
    private val boardManager = BoardStateManager(db, p1Connection, p2connection)

    val id = boardManager.gameID

    suspend fun mainLoop(player: Player) {
        val prefix = "[Game $id][Player ${if (player == Player.Player1) 1 else 2}] "
        val connection = if (player == Player.Player1) p1Connection else p2connection
        val otherConnection = if (player == Player.Player1) p2connection else p1Connection

        println(prefix + "Starting game")
        println(prefix + "Sending game rules to client")
        connection.sendPacket(RuleInfoPacket())
        println(prefix + "Sending match to client")

        connection.sendPacket(
            MatchFoundPacket(otherConnection.getUserInfo(), id, false, player == Player.Player1),
        )

        /*
         * Deck Master Select Phase -> Keep looping until both players are ready.
         */
        var playerDeckMasterId = -1
        var imReady = false
        var theirReady = false

        while (!(imReady && theirReady) && connection.isOpen) {
            val packet = connection.receivePacket()
            when (packet) {
                null -> {
                    if (connection.isOpen) connection.close()
                    println(prefix + "Connection was closed unexpectedly")

                    if (otherConnection.isOpen) {
                        println(prefix + "Informing opponent")
                        otherConnection.sendPacket(
                            DisconnectPacket(
                                DisconnectPacket.Reason.opponent_disconnect,
                                "The opponent has closed it's connection",
                            ),
                        )
                        // otherConnection.close()
                    }
                }
                is DeckMasterRequestPacket -> {
                    playerDeckMasterId = boardManager.handleDeckMasterRequest(player, packet)
                    if (playerDeckMasterId != -1) {
                        imReady = true
                        connection.sendPacket(DeckMasterSelectedPacket(true, true))

                        // Let the opponent know that player is ready.
                        otherConnection.sendPacket(DeckMasterSelectedPacket(true, false))
                    } else {
                        assert(false) { "Server received invalid card ID." }
                    }
                }
                is OpponentReadyPacket -> {
                    theirReady = true
                }
                else -> {
                    connection.sendPacket(
                        UnknownPacketPacket(
                            "Received an unexpected packet type.",
                        ),
                    )
                    println(prefix + "Received unknown packet")
                }
            }
        }

        println("Sending Game Start Packet to...$connection, ${player == Player.Player1}")
        connection.sendPacket(GameStartPacket())

        imReady = false
        theirReady = false

        /*
         * Wait until both clients have loaded into main game scene before sending 'setup' packets.
         */
        while (!(imReady && theirReady) && connection.isOpen) {
            val packet = connection.receivePacket()
            when (packet) {
                null -> {
                    if (connection.isOpen) connection.close()
                    println(prefix + "Connection was closed unexpectedly")

                    if (otherConnection.isOpen) {
                        println(prefix + "Informing opponent")
                        otherConnection.sendPacket(
                            DisconnectPacket(
                                DisconnectPacket.Reason.opponent_disconnect,
                                "The opponent has closed it's connection",
                            ),
                        )
                        // otherConnection.close()
                    }
                }
                is PlayerReadyPacket -> {
                    otherConnection.sendPacket(OpponentReadyPacket())
                    imReady = true
                }
                is OpponentReadyPacket -> {
                    theirReady = true
                }
                else -> {
                    connection.sendPacket(
                        UnknownPacketPacket(
                            "Received an unexpected packet type.",
                        ),
                    )
                    println(prefix + "Received unknown packet")
                }
            }
        }

        /*
         * Add Deck Master onto game board.
         */
        boardManager.initDeckMaster(player, playerDeckMasterId)

        for (i in 0..<4) {
            boardManager.drawCard(player)
        }

        if (player == Player.Player1) {
            connection.sendPacket(StartTurnPacket())
            boardManager.drawCard(player)
        }

        /*
         * Main Game Phase
         */
        while (connection.isOpen) {
            val packet = connection.receivePacket()
            when (packet) {
                null -> {
                    if (connection.isOpen) connection.close()
                    println(prefix + "Connection was closed unexpectedly")

                    if (otherConnection.isOpen) {
                        println(prefix + "Informing opponent")
                        otherConnection.sendPacket(
                            DisconnectPacket(
                                DisconnectPacket.Reason.opponent_disconnect,
                                "The opponent has closed it's connection",
                            ),
                        )
                        // otherConnection.close()
                    }
                }
                is GetBoardStatePacket -> {
                    println(prefix + "getboardstate")
                    connection.sendPacket(GetBoardStateResponse(boardManager.getBoardState()))
                }
                is AttackRequestPacket -> {
                    boardManager.handleAttackPacket(packet, player)
                }
                is SummonRequestPacket -> {
                    boardManager.handleSummonPacket(packet, player)
                }
                is SwitchPlaceRequestPacket -> {
                    boardManager.handleSwitchPlacePacket(packet, player)
                }
                is EndTurnPacket -> {
                    boardManager.handleEndTurn(player)
                }
                is DrawCardRequestPacket -> {
                    boardManager.handleDrawCard(player)
                }
                is UseAbilityRequestPacket -> {
                    boardManager.handleUseAbilityPacket(packet, player)
                }
                is UseMagicCardRequestPacket -> {
                    boardManager.handleUseMagicCardPacket(packet, player)
                }
                else -> {
                    connection.sendPacket(UnknownPacketPacket("unknown packet type received"))
                    println(prefix + "Received unknown packet")
                }
            }

            boardManager.updatePassives(packet, player)

            boardManager.gameOverHandler()
            println(
                prefix +
                    "new ram: ${boardManager.getBoardState().ram[0]}, ${boardManager.getBoardState().ram[1]}  max: ${boardManager
                        .getBoardState()
                        .max_ram[0]}, ${boardManager.getBoardState().max_ram[1]}",
            )

            println("connection states ${connection.isOpen}, ${otherConnection.isOpen}")
        }
    }
}
