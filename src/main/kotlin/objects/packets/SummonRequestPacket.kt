package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.SUMMON_REQUEST)
class SummonRequestPacket(
    @Required val card_id: Int,
    @Required val position: CardPosition
) : Packet() {

    fun getResponsePacket(is_you: Boolean, valid: Boolean, new_card: CardState?, new_ram: Int): SummonPacket {
        return SummonPacket(is_you, valid, position, new_card, new_ram)
    }

}

@Serializable
@SerialName(PacketType.SUMMON)
class SummonPacket(
    @Required val is_you: Boolean,
    @Required val valid: Boolean,
    @Required val position: CardPosition?,
    @Required val new_card: CardState?,
    @Required val new_ram: Int
) : Packet() {
    init {
        require(valid == (new_card != null))
    }
}
