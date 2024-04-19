package objects


enum class ClientCommandType {
    Ping,
    Exit,
    Message,
    GameEvent,
    Version,
    Attack,
    Summon,
    Cards,
}
class ClientCommand(val type: ClientCommandType) {
    /**
     * Valid for types Message, GameEvent
     */
    var message: String? = null

    /**
     * Valid for types Attack, Summon
     */
    var row: Int = -1
    /**
     * Valid for types Attack, Summon
     */
    var column: Int = -1
    /**
     * Valid for types Attack
     */
    var targetRow: Int = -1
    /**
     * Valid for types Attack
     */
    var targetColumn: Int = -1

    /**
     * Valid for types Summon
     */
    var cardIndex: Int = -1
}
