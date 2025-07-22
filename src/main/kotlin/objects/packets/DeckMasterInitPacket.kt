/*
 * Created by BSGMatt on 2025.1.16
 */

package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

/**
 * Packet for initializing a deck master on the client side.
 */
@Serializable
@SerialName(PacketType.DECK_MASTER_INIT)
@Suppress("PropertyName")
class DeckMasterInitPacket(
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val position: CardPosition?,
    @Required val new_card: CardState?,
) : Packet()
