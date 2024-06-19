package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.GET_BOARD_STATE)
class GetBoardStatePacket(
    @Required val reason: Reason
) : Packet() {
    val response_id: Int = generateResponseID()

    fun getResponsePacket(board: BoardState): Response {
        return Response(response_id, board)
    }

    @Serializable
    @SerialName(PacketType.GET_BOARD_STATE_RESPONSE)
    class Response(
        @Required val response_id: Int,
        @Required val board: BoardState
    ) : Packet() {
    }

    enum class Reason {
        state_conflict,
        reconnect,
        connect,
        debug,
    }
}
