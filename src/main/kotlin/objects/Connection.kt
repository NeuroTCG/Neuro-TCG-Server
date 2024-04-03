package objects

import kotlinx.coroutines.*
import java.io.*
import java.net.*

class Connection {
    suspend fun start() {
        val serverSocket = withContext(Dispatchers.IO) {ServerSocket(9933)}
        println("Listening for clients...")
        val clientSocket = withContext(Dispatchers.IO) {serverSocket.accept()}
        val clientSocketIP: String = clientSocket.inetAddress.toString()
        val clientSocketPort: Int = clientSocket.port
        println("[IP: $clientSocketIP ,Port: $clientSocketPort]  Client classes.Connection Successful!")
        val dataIn: DataInputStream = DataInputStream(clientSocket.inputStream)
        val dataOut: DataOutputStream = DataOutputStream(clientSocket.outputStream)
        withContext(Dispatchers.IO){dataOut.writeUTF("Connected to the server")}
        var exitConnection: Boolean = false
        var serverMessage: String = ""

        while (!exitConnection) {
            val clientMessage: String?;
            try {
                clientMessage = withContext(Dispatchers.IO){dataIn.readUTF()}
            }
            catch (e: SocketException){
                println("Client reset the connection");
                break;
            }
            catch (e: EOFException){
                println("Client closed the connection unexpectedly");
                break;
            }
            val serverResponse: MutableList<Any> = withContext(Dispatchers.IO) {Parser().parse(clientMessage)}
            serverMessage = serverResponse[0] as String
            exitConnection = serverResponse[1] as Boolean
            println(clientMessage)
            withContext(Dispatchers.IO) {dataOut.writeUTF(serverMessage)}
        }
        stop(dataIn, dataOut, serverSocket, clientSocket)
    }
    private suspend fun stop(dataIn: DataInputStream, dataOut: DataOutputStream, serverSocket: ServerSocket, clientSocket: Socket) {
        println("Disconnecting...")
        withContext(Dispatchers.IO) {dataIn.close()}
        withContext(Dispatchers.IO) {dataOut.close()}
        withContext(Dispatchers.IO) {serverSocket.close()}
        withContext(Dispatchers.IO) {clientSocket.close()}
    }
}
