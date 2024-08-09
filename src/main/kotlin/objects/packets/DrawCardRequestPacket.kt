package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.DRAW_CARD_REQUEST)
/**
 * Sent by: Client
 */
class DrawCardRequestPacket : Packet()

@Serializable
@SerialName(PacketType.DRAW_CARD)
@Suppress("PropertyName")
/**
 * Sent by: Server
 *
 * @param card_id  if negative, the action was invalid
 */
class DrawCard(
    @Required val card_id: Int,
    @Required val is_you: Boolean
) : Packet()
