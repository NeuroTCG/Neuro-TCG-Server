import objects.Game
import java.net.*

fun main() {
    while (true) {
        val serverSocket = ServerSocket(9933)
        println("Listening for clients...")

        val clientSocket = serverSocket.accept()
        val clientSocketIP: String = clientSocket.inetAddress.toString()
        val clientSocketPort: Int = clientSocket.port
        println("[IP: $clientSocketIP ,Port: $clientSocketPort]  Client Connection Successful!")

        val game = Game(clientSocket)
        game.mainLoop()

        serverSocket.close()
    }
}
