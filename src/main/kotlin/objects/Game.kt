package objects

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import objects.shared.*
import java.net.*

class Game(clientSocket: Socket) {
    val connection = Connection(clientSocket)
    val boardManager = BoardStateManager()
    val supportedVersions = listOf("early development build")
    val recommendedVersion = "early development build"


    fun mainLoop() {
        connection.writeMessage("Hello from the server")

        while (connection.isOpen) {
            val clientMessage = connection.readMessage()
            if (clientMessage == null) {
                println("Client closed the connection unexpectedly")
                connection.close()
                break
            }

            val command = Parser().parse(clientMessage)
            when (command.type) {
                ClientCommandType.Ping -> {
                    connection.writeMessage("pong")
                    println("Ping from client")
                }
                ClientCommandType.Cards -> {
                    connection.writeMessage(Json.encodeToString(CardStats.cardIDMapping))
                }

                ClientCommandType.Exit -> {
                    connection.close()
                    println("Connection closed by client")
                }

                ClientCommandType.GameEvent -> {
                    println("Received game event packet with content: ${command.message}")
                    connection.writeMessage("Received game event packet with content ${command.message}")
                }

                ClientCommandType.Version -> {
                    if (supportedVersions.contains(command.message)) {
                        if (command.message != recommendedVersion) {
                            println("client with version ${command.message} connected")
                            connection.writeMessage("non-recommended version")
                        } else {
                            println("client with recommended version connected")
                            connection.writeMessage("version OK")
                        }
                    } else {
                        println("unsupported client attempted to connect")
                        connection.writeMessage("unsupported version")
                    }
                }

                ClientCommandType.Message -> println("Message from client: ${command.message}")
                ClientCommandType.Attack -> {
                    try {
                        boardManager.attack(
                            0,
                            command.row,
                            command.column,
                            command.targetRow,
                            command.targetColumn
                        )
                        connection.writeMessage("valid")
                        val json = Json.encodeToString(boardManager.getBoardState())
                        connection.writeMessage(json)
                        println(json)
                    } catch (e: InvalidMoveException) {
                        connection.writeMessage("invalid")
                        println(e.message)
                    }
                }

                ClientCommandType.Summon -> {
                    try {
                        boardManager.summon(
                            0,
                            command.row,
                            command.column,
                            command.cardIndex
                        )
                        connection.writeMessage("valid")
                        val json = Json.encodeToString(boardManager.getBoardState())
                        connection.writeMessage(json)
                        println(json)
                    } catch (e: InvalidMoveException) {
                        connection.writeMessage("invalid")
                        println(e.message)
                    }
                }
            }
        }
    }
}

