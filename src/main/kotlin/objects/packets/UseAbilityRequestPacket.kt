package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.USE_ABILITY_REQUEST)
class UseAbilityRequestPacket(
    @Required val target_position: CardPosition,
    @Required val ability_position: CardPosition,
) : Packet() {

    fun getResponsePacket(is_you: Boolean, valid: Boolean, target_card: CardState?, ability_card: CardState?): UseAbilityPacket {
        return UseAbilityPacket(is_you, valid, target_position, ability_position, target_card, ability_card)
    }

}

@Serializable
@SerialName(PacketType.USE_ABILITY)
class UseAbilityPacket(
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val target_position: CardPosition?,
    @Required val ability_position: CardPosition?,
    @Required val target_card: CardState?,
    @Required val ability_card: CardState?,
) : Packet()
