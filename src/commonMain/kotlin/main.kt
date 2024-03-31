import kotlinx.coroutines.*
import java.io.*
import java.net.*

suspend fun main() {
    val serverSocket = withContext(Dispatchers.IO) {
        ServerSocket(9933)
    }
    println("Listening for clients...")
    val clientSocket = withContext(Dispatchers.IO) {
        serverSocket.accept()
    }
    val clientSocketIP: String = clientSocket.inetAddress.toString()
    val clientSocketPort: Int = clientSocket.port
    println("[IP: $clientSocketIP ,Port: $clientSocketPort]  Client Connection Successful!")

    val dataIn: DataInputStream = DataInputStream(clientSocket.inputStream)
    val dataOut: DataOutputStream = DataOutputStream(clientSocket.outputStream)
    var exitConnection: Boolean = false

    while (!exitConnection){
        val clientMessage: String = withContext(Dispatchers.IO) {
            dataIn.readUTF()
        }
        var serverMessage: String
        if (clientMessage == "exit") {
            println("client command 'EXIT'")
            serverMessage = "client commanded server to exit"
            exitConnection = true
        } else if (clientMessage == "ping") {
            println("client command 'PING'")
            serverMessage = "pong"
        } else {
            serverMessage = "Hi this is coming from Server!"
        }

        println(clientMessage)
        withContext(Dispatchers.IO) {
            dataOut.writeUTF(serverMessage)
        }
    }

    withContext(Dispatchers.IO) {
        dataIn.close()
    }
    withContext(Dispatchers.IO) {
        dataOut.close()
    }
    withContext(Dispatchers.IO) {
        serverSocket.close()
    }
    withContext(Dispatchers.IO) {
        clientSocket.close()
    }
}
