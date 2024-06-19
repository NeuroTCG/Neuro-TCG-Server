package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.SUMMON)
class SummonPacket(
    @Required val card_id: Int, @Required val position: CardPosition
) : Packet() {
    val response_id: Int = generateResponseID()

    fun getResponsePacket(valid: Boolean, new_card: CardState?): Response {
        return Response(valid, response_id, new_card)
    }

    fun getOpponentPacket(new_card: CardState): Opponent {
        return Opponent(position, new_card)
    }


    @Serializable
    @SerialName(PacketType.SUMMON_RESPONSE)
    class Response(
        @Required val valid: Boolean,
        @Required val response_id: Int,
        @Required val new_card: CardState?
    ) : Packet() {
        init {
            require(valid == (new_card != null))
        }
    }

    @Serializable
    @SerialName(PacketType.SUMMON_OPPONENT)
    class Opponent(
        @Required val position: CardPosition,
        @Required val new_card: CardState
    ) : Packet()

}
