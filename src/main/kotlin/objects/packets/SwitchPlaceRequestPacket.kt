package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.SWITCH_PLACE_REQUEST)
/**
 * Sent by: Client
 *
 * Tries to swap two cards or a card and null.
 */
class SwitchPlaceRequestPacket(
    @Required val position1: CardPosition,
    @Required val position2: CardPosition
) : Packet() {

    fun getResponsePacket(isYou: Boolean, valid: Boolean): SwitchPlacePacket {
        return SwitchPlacePacket(isYou, valid, position1, position2)
    }

}

@Serializable
@SerialName(PacketType.SWITCH_PLACE)
@Suppress("PropertyName")
/**
 * Sent by: Server
 *
 * Informs the client that two cards or a card and `null` have swapped places.
 */
class SwitchPlacePacket(
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val position1: CardPosition,
    @Required val position2: CardPosition,
) : Packet()
