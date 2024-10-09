package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

/**
 * Sent by: Client
 *
 * @param target_position the card of the opponent to attack
 * @param attacker_position the card of the player that does the attack
 */
@Serializable
// https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md
@SerialName(PacketType.ATTACK_REQUEST)
@Suppress("PropertyName")
class AttackRequestPacket(
    @Required val target_position: CardPosition,
    @Required val attacker_position: CardPosition,
) : Packet() {
    fun getResponsePacket(
        isYou: Boolean,
        valid: Boolean,
        targetCard: CardState?,
        attackerCard: CardState?,
    ): AttackPacket = AttackPacket(isYou, valid, target_position, attacker_position, targetCard, attackerCard)
}

/**
 *
 * Sent by: Server
 *
 * Informs the client of an attack by either it or the opponent.
 *
 * If any of the cards were killed by this attack, they will be set to `null`.
 */
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
) : Packet()
