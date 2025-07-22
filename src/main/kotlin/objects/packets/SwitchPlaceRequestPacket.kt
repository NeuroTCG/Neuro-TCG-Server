package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

/**
 * Sent by: Client
 *
 * Tries to swap two cards or a card and null.
 */
@Serializable
@SerialName(PacketType.SWITCH_PLACE_REQUEST)
class SwitchPlaceRequestPacket(
    @Required val response_id: Int,
    @Required val position1: CardPosition,
    @Required val position2: CardPosition,
) : Packet() {
    fun getResponsePacket(
        isYou: Boolean,
        valid: Boolean,
    ): SwitchPlacePacket =
        SwitchPlacePacket(
            if (isYou) {
                response_id
            } else {
                -1
            },
            isYou,
            valid,
            position1,
            position2,
        )
}

/**
 * Sent by: Server
 *
 * Informs the client that two cards or a card and `null` have swapped places.
 */
@Serializable
@SerialName(PacketType.SWITCH_PLACE)
@Suppress("PropertyName")
class SwitchPlacePacket(
    @Required val response_id: Int,
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val position1: CardPosition,
    @Required val position2: CardPosition,
) : Packet()
