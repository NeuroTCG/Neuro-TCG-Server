package objects

import java.net.Socket

class Game(clientSocket: Socket) {
    val connection = Connection(clientSocket)
    val boardState = BoardState()

    fun mainLoop(){
        connection.writeMessage("Hello from the server")

        while (connection.isOpen) {
            val clientMessage = connection.readMessage()
            if (clientMessage == null){
                println("Client closed the connection unexpectedly")
                connection.close()
                break
            }

            val command = Parser().parse(clientMessage)
            when (command.type){
                ClientCommandType.Ping -> {
                    connection.writeMessage("pong")
                    println("Ping from client")
                }
                ClientCommandType.Exit -> {
                    connection.close()
                    println("Connection closed by client")
                }
                ClientCommandType.Message -> println("Message from client: ${command.message}")
            }
        }
    }
}

