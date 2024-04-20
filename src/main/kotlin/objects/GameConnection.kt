package objects

import java.io.*
import java.net.*
class GameConnection(socket: Socket) {
    private val clientSocket = socket
    private val dataIn = DataInputStream(clientSocket.inputStream)
    private val dataOut = DataOutputStream(clientSocket.outputStream)

    var isOpen = true
        private set

    fun readMessage(): String? {
        return try {
            dataIn.readUTF()
        } catch (e: SocketException){
            null
        } catch (e: EOFException){
            null
        }
    }
    fun writeMessage(msg: String) {
        dataOut.writeUTF(msg)
    }

    fun close() {
        println("Disconnecting...")
        dataIn.close()
        dataOut.flush()
        dataOut.close()
        clientSocket.close()
        isOpen = false
    }
}
