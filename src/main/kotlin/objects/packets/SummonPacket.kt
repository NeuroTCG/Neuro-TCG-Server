package objects.packets

import kotlinx.serialization.*

@Serializable
class SummonPacket(
    @Required val card_id: Int, @Required val position: BoardPosition
) : Packet(PacketType.summon) {
    val response_id: Int = generateResponseID()

    fun getResponsePacket(valid: Boolean, new_card: FullCardState?): Response {
        return Response(valid, response_id, new_card)
    }

    fun getOpponentPacket(new_card: FullCardState): Opponent {
        return Opponent(position, new_card)
    }


    @Serializable
    class Response(
        @Required val valid: Boolean,
        @Required val response_id: Int,
        @Required val new_card: FullCardState?
    ) : Packet(PacketType.summon_response) {
        init {
            require(valid == (new_card != null))
        }
    }

    @Serializable
    class Opponent(
        @Required val position: BoardPosition,
        @Required val new_card: FullCardState
    ) : Packet(PacketType.summon_opponent)

}
