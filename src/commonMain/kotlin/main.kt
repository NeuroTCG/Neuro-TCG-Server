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
    val clientSocketIP = clientSocket.inetAddress.toString()
    val clientSocketPort = clientSocket.port
    println("[IP: $clientSocketIP ,Port: $clientSocketPort]  Client Connection Successful!")

    val dataIn = DataInputStream(clientSocket.inputStream)
    val dataOut = DataOutputStream(clientSocket.outputStream)

    val clientMessage = withContext(Dispatchers.IO) {
        dataIn.readUTF()
    }
    println(clientMessage)
    val serverMessage = "Hi this is coming from Server!"
    withContext(Dispatchers.IO) {
        dataOut.writeUTF(serverMessage)
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
