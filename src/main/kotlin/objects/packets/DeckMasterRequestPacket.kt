/*
 * Created by BSGMatt on 2025.1.21
 */

package objects.packets

import kotlinx.serialization.*

/**
 * Packet for initializing a deck master on the client side.
 */
@Serializable
@SerialName(PacketType.DECK_MASTER_REQUEST)
@Suppress("PropertyName")
class DeckMasterRequestPacket(
    @Required val card_id: Int,
) : Packet()
