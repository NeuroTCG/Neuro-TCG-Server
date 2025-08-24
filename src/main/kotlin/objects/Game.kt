package objects

import SuspendingCyclicBarrier
import objects.packets.*

class Game(
    val p1Connection: GameConnection,
    val p2connection: GameConnection,
    db: GameDatabase,
) {
    private val boardManager = BoardStateManager(db, p1Connection, p2connection)

    val id = boardManager.gameID

    var readyBarrier = SuspendingCyclicBarrier(2)

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
                                "The opponent has closed their connection",
                            ),
                        )
                        otherConnection.close()
                        readyBarrier.reset()
                        return
                    }
                }
                is DeckMasterRequestPacket -> {
                    playerDeckMasterId = boardManager.handleDeckMasterRequest(player, packet)
                    check(playerDeckMasterId != -1) { "Server received invalid card ID." }
                    connection.sendPacket(DeckMasterSelectedPacket(packet.response_id, true, true))

                    // Let the opponent know that player is ready.
                    otherConnection.sendPacket(DeckMasterSelectedPacket(-1, true, false))
                    break
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

        readyBarrier.await()

        println(prefix + "Sending Game Start Packet to...$connection, ${player == Player.Player1}")
        connection.sendPacket(GameStartPacket())

        /*
         * Wait until both clients have loaded into main game scene before sending 'setup' packets.
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
                                "The opponent has closed their connection",
                            ),
                        )
                        otherConnection.close()
                        readyBarrier.reset()
                        return
                    }
                }
                is PlayerReadyPacket -> {
                    otherConnection.sendPacket(OpponentReadyPacket())
                    break
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

        readyBarrier.await()
        println(prefix + "Setting up initial game state")

        /*
         * Add Deck Master onto game board.
         */
        boardManager.initDeckMaster(player, playerDeckMasterId)

        for (i in 0..<4) {
            boardManager.drawCard(player, null)
        }

        if (player == Player.Player1) {
            connection.sendPacket(StartTurnPacket())
            boardManager.drawCard(player, null)
        }

        readyBarrier.await()

        boardManager.initPassives(player)

        println(prefix + "Game is starting")

        println(boardManager.getVersusString())

        /*
         * Main Game Phase
         */
        while (connection.isOpen) {
            val packet = connection.receivePacket()
            when (packet) {
                null -> {
                    println(prefix + "Connection was closed unexpectedly")
                    if (connection.isOpen) connection.close()

                    if (otherConnection.isOpen) {
                        println(prefix + "Informing opponent")
                        otherConnection.sendPacket(
                            DisconnectPacket(
                                DisconnectPacket.Reason.opponent_disconnect,
                                "The opponent has closed their connection",
                            ),
                        )
                        otherConnection.close()
                        readyBarrier.reset()
                        return
                    }
                }
                is GetBoardStatePacket -> {
                    println(prefix + "getboardstate")
                    connection.sendPacket(GetBoardStateResponse(packet.response_id, boardManager.getBoardState()))
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
                    boardManager.handleDrawCard(packet, player)
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
