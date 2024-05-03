package objects

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.io.*
import java.net.*

class GameConnection(socket: DefaultWebSocketServerSession) {
    private val clientSocket = socket

    var isOpen = true
        private set

    fun readMessage(): String? {
        return try {
            runBlocking { clientSocket.incoming.receive().data.decodeToString() }
        } catch (e: SocketException) {
            null
        } catch (e: EOFException) {
            null
        }
    }

    fun writeMessage(msg: String) {
        runBlocking {
            clientSocket.send(msg)
            clientSocket.flush()
        }
    }

    fun close() {
        println("Disconnecting...")
        runBlocking { clientSocket.close(CloseReason(CloseReason.Codes.NORMAL, "Bye")) }
        isOpen = false
    }
}
