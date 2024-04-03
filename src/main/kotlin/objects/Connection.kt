package objects

import java.io.*
import java.net.*
class Connection(socket: Socket) {
    private val clientSocket = socket
    private val dataIn = DataInputStream(clientSocket.inputStream)
    private val dataOut = DataOutputStream(clientSocket.outputStream)

    var isOpen = true
        private set

    fun readMessage(): String? {
        try {
            return dataIn.readUTF()
        }
        catch (e: SocketException){
            return null
        }
        catch (e: EOFException){
            return null
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
