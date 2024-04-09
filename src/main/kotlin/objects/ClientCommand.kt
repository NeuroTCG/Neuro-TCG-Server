package objects


enum class ClientCommandType {
    Ping,
    Exit,
    Message,
    GameEvent,
    Version,
}
class ClientCommand(val type: ClientCommandType) {
    /**
     * Valid for types Message
     */
    var message: String? = null
}
