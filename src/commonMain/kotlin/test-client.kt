import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
fun main() {
    val socket = Socket()
    socket.connect(InetSocketAddress("127.0.0.1", 9933), 1000)
    println("Connection Successful!")
    val dataIn = DataInputStream(socket.inputStream)
    val dataOut = DataOutputStream(socket.outputStream)
    dataOut.writeUTF("Hello, This is coming from Client!")
    val serverMessage = dataIn.readUTF()
    println(serverMessage)

    dataIn.close()
    dataOut.close()
    socket.close()
}
