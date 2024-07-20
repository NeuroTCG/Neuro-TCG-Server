package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.SWITCH_PLACE_REQUEST)
class SwitchPlaceRequestPacket(
    @Required val position1: CardPosition,
    @Required val position2: CardPosition
) : Packet() {

    fun getResponsePacket(is_you: Boolean, valid: Boolean): SwitchPlacePacket {
        return SwitchPlacePacket(is_you, valid, position1, position2)
    }

}

@Serializable
@SerialName(PacketType.SWITCH_PLACE)
class SwitchPlacePacket(
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val position1: CardPosition,
    @Required val position2: CardPosition,
) : Packet()
