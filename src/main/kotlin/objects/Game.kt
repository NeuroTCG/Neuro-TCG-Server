package objects

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import objects.packets.*
import objects.packets.objects.BoardState
import objects.passives.*
import java.io.File

class Game(
    val p1Connection: GameConnection,
    val p2connection: GameConnection,
    db: GameDatabase,
) {
    private val boardManager = BoardStateManager(db, p1Connection, p2connection)

    val id = boardManager.gameID

    companion object {
        val DEBUG_EVENTS_ENABLED = true
        val DEBUG_EVENT_SAVE_FILE = "data/saved_game.json"
    }

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

        for (i in 0..<4) {
            boardManager.drawCard(player)
        }

        if (player == Player.Player1) {
            connection.sendPacket(StartTurnPacket())
            boardManager.drawCard(player)
        }

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
                is DebugEventPacket -> {
                    if (DEBUG_EVENTS_ENABLED) {
                        when (packet.event) {
                            "save" -> {
                                println(prefix + "[DEBUG EVENT] Saving game")

                                File(DEBUG_EVENT_SAVE_FILE).writeText(Json.encodeToString(boardManager.getBoardState()))
                            }
                            "load" -> {
                                println(prefix + "[DEBUG EVENT] Loading game")
                                

                                boardManager.loadGame(Json.decodeFromString<BoardState>(File(DEBUG_EVENT_SAVE_FILE).readText()))
                            }
                            else -> {
                                println(prefix + "Received unknown debug event '${packet.event}'")
                            }
                        }
                    }
                }
                else -> {
                    connection.sendPacket(UnknownPacketPacket("unknown packet type received"))
                    println(prefix + "Received unknown packet")
                }
            }

            val passiveUpdates = boardManager.passiveManager.updatePassives(packet)
            if (passiveUpdates != null) {
                connection.sendPacket(passiveUpdates);
            }

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
