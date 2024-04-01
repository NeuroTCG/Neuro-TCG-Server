package objects

class Parser {
    suspend fun parse(clientMessage: String): MutableList<Any> {
        val clientMessageArray: List<String> = clientMessage.split(",")
        val clientCommand: String = clientMessageArray[0]
        var serverMessage: String = ""
        var exitConnection: Boolean = false
        when (clientCommand) {
            "exit" -> {
                println("client command 'EXIT'")
                serverMessage = "client commanded server to exit"
                exitConnection = true
            }
            "ping" -> {
                println("client command 'PING'")
                serverMessage = "pong"
            }
        }
    return mutableListOf<Any>(serverMessage, exitConnection)
    }
}
