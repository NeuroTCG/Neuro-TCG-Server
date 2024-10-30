package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.USE_MAGIC_CARD_REQUEST)
@Suppress("PropertyName")
class UseMagicCardRequestPacket(
    @Required val target_position: CardPosition?,
    @Required val card_id: Int,
    @Required val hand_pos: Int,
) : Packet() {
    fun getResponsePacket(
        isYou: Boolean,
        valid: Boolean,
        ability: Ability?,
        target_card: CardState?,
    ): UseMagicCardPacket = UseMagicCardPacket(isYou, valid, hand_pos, ability, target_position, target_card)
}

@Serializable
@SerialName(PacketType.USE_MAGIC_CARD)
@Suppress("PropertyName")
class UseMagicCardPacket(
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val hand_pos: Int,
    @Required val ability: Ability?,
    @Required val target_position: CardPosition?,
    @Required val target_card: CardState?,
) : Packet()
