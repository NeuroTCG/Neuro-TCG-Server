package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.GET_GAME_STATE)
class GetGameStatePacket(
    @Required val reason: Reason
) : Packet() {
    val response_id: Int = generateResponseID()

    enum class Reason {
        state_conflict,
        reconnect,
        connect,
        debug,
    }
}
