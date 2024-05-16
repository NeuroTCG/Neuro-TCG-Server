package objects.packets

import kotlinx.serialization.*

@Serializable
class GetGameStatePacket(
    @Required val reason: Reason
) : Packet(PacketType.get_game_state) {
    val response_id: Int = generateResponseID()

    enum class Reason{
        state_conflict,
        reconnect,
        connect,
        debug,
    }
}
