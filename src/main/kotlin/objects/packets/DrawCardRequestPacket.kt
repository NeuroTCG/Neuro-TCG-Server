package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.DRAW_CARD_REQUEST)
class DrawCardRequestPacket : Packet()

@Serializable
@SerialName(PacketType.DRAW_CARD)
@Suppress("PropertyName")
class DrawCard(
    @Required val card_id: Int,
    @Required val is_you: Boolean
) : Packet()
