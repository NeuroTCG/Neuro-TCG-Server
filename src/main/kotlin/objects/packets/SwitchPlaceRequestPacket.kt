package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.SWITCH_PLACE_REQUEST)
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
class SwitchPlacePacket(
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val position1: CardPosition,
    @Required val position2: CardPosition,
) : Packet()
