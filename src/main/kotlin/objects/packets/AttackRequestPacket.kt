package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
// https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md
@SerialName(PacketType.ATTACK_REQUEST)
class AttackRequestPacket(
    @Required val target_position: CardPosition,
    @Required val attacker_position: CardPosition
) : Packet() {

    fun getResponsePacket(is_you: Boolean, valid: Boolean, target_card: CardState?, attacker_card: CardState?): AttackPacket {
        return AttackPacket(is_you, valid, target_position, attacker_position, target_card, attacker_card)
    }

}

@Serializable
@SerialName(PacketType.ATTACK)
class AttackPacket(
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val target_position: CardPosition?,
    @Required val attacker_position: CardPosition?,
    @Required val target_card: CardState?,
    @Required val attacker_card: CardState?,
) : Packet()
