package objects


enum class ClientCommandType {
    Ping,
    Exit,
    Message,
}
class ClientCommand(val type: ClientCommandType) {
    /**
     * Valid for types Message
     */
    var message: String? = null
}
