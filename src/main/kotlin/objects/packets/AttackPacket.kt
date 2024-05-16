package objects.packets

import kotlinx.serialization.*

@Serializable
class AttackPacket(
    @Required val target_position: BoardPosition,
    @Required val attacker_position: BoardPosition
) : Packet(PacketType.attack) {
    val response_id: Int = generateResponseID()

    fun getResponsePacket(valid: Boolean, target_card: FullCardState?, attacker_card: FullCardState?): Response {
        return Response(valid, response_id, target_card, attacker_card)
    }

    fun getOpponentPacket(target_card: FullCardState?, attacker_card: FullCardState?): Opponent {
        return Opponent(target_position, attacker_position, target_card, attacker_card)
    }


    @Serializable
    class Response(
        @Required val valid: Boolean,
        @Required val response_id: Int,
        @Required val target_card: FullCardState?,
        @Required val attacker_card: FullCardState?,
    ) : Packet(PacketType.attack_response)

    @Serializable
    class Opponent(
        @Required val target_position: BoardPosition,
        @Required val attacker_position: BoardPosition,
        @Required val target_card: FullCardState?,
        @Required val attacker_card: FullCardState?,
    ) : Packet(PacketType.attack_opponent)

}
