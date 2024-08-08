package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
// https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md
@SerialName(PacketType.ATTACK_REQUEST)
@Suppress("PropertyName")
class AttackRequestPacket(
    @Required val target_position: CardPosition,
    @Required val attacker_position: CardPosition,
    @Required val counterattack: Boolean
) : Packet() {

    fun getResponsePacket(isYou: Boolean, valid: Boolean, targetCard: CardState?, attackerCard: CardState?, counterattack: Boolean): AttackPacket {
        return AttackPacket(isYou, valid, target_position, attacker_position, targetCard, attackerCard, counterattack)
    }

}

@Serializable
@SerialName(PacketType.ATTACK)
@Suppress("PropertyName")
class AttackPacket(
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val target_position: CardPosition?,
    @Required val attacker_position: CardPosition?,
    @Required val target_card: CardState?,
    @Required val attacker_card: CardState?,
    @Required val counterattack: Boolean,
) : Packet()
