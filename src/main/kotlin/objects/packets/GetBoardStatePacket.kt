package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.GET_BOARD_STATE)
class GetBoardStatePacket(
    @Required val reason: Reason
) : Packet() {


    enum class Reason {
        state_conflict,
        reconnect,
        connect,
        debug,
    }
}

@Serializable
@SerialName(PacketType.GET_BOARD_STATE_RESPONSE)
class GetBoardStateResponse(
    @Required val board: BoardState
) : Packet() {}
