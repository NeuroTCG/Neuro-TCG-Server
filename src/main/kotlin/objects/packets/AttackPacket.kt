package objects.packets

import kotlinx.serialization.*

@Serializable
// https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md
@SerialName(PacketType.ATTACK)
class AttackPacket(
    @Required val target_position: CardPosition,
    @Required val attacker_position: CardPosition
) : Packet() {
    @Required
    val response_id: Int = generateResponseID()

    fun getResponsePacket(valid: Boolean, target_card: FullCardState?, attacker_card: FullCardState?): Response {
        return Response(valid, response_id, target_card, attacker_card)
    }

    fun getOpponentPacket(target_card: FullCardState?, attacker_card: FullCardState?): Opponent {
        return Opponent(target_position, attacker_position, target_card, attacker_card)
    }


    @Serializable
    @SerialName(PacketType.ATTACK_RESPONSE)
    class Response(
        @Required val valid: Boolean,
        @Required val response_id: Int,
        @Required val target_card: FullCardState?,
        @Required val attacker_card: FullCardState?,
    ) : Packet()

    @Serializable
    @SerialName(PacketType.ATTACK_OPPONENT)
    class Opponent(
        @Required val target_position: CardPosition,
        @Required val attacker_position: CardPosition,
        @Required val target_card: FullCardState?,
        @Required val attacker_card: FullCardState?,
    ) : Packet()

}
