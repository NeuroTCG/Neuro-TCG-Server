package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.USE_ABILITY_REQUEST)
@Suppress("PropertyName")
class UseAbilityRequestPacket(
    @Required val target_position: CardPosition,
    @Required val ability_position: CardPosition,
) : Packet() {
    fun getResponsePacket(
        isYou: Boolean,
        valid: Boolean,
        targetCard: CardState?,
        abilityCard: CardState?,
    ): UseAbilityPacket = UseAbilityPacket(isYou, valid, target_position, ability_position, targetCard, abilityCard)
}

@Serializable
@SerialName(PacketType.USE_ABILITY)
@Suppress("PropertyName")
class UseAbilityPacket(
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val target_position: CardPosition?,
    @Required val ability_position: CardPosition?,
    @Required val target_card: CardState?,
    @Required val ability_card: CardState?,
) : Packet()
