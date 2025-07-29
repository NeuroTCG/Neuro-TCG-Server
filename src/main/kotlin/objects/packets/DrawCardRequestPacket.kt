package objects.packets

import kotlinx.serialization.*

/**
 * Sent by: Client
 */
@Serializable
@SerialName(PacketType.DRAW_CARD_REQUEST)
class DrawCardRequestPacket(
    @Required val response_id: Int,
) : Packet()

/**
 * Sent by: Server
 *
 * @param card_id  if negative, the action was invalid
 */
@Serializable
@SerialName(PacketType.DRAW_CARD)
@Suppress("PropertyName")
class DrawCard(
    @Required val response_id: Int,
    @Required val card_id: Int,
    @Required val is_you: Boolean,
) : Packet()
