/*
 * Created by BSGMatt on 2025.1.21
 */

package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.DECK_MASTER_SELECTED)
@Suppress("PropertyName")
class DeckMasterSelectedPacket(
    @Required val valid: Boolean,
    @Required val is_you: Boolean,
) : Packet()
