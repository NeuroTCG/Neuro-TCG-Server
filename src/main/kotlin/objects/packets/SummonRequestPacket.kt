package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.SUMMON_REQUEST)
@Suppress("PropertyName")
class SummonRequestPacket(
    @Required val card_id: Int,
    @Required val position: CardPosition
) : Packet() {

    fun getResponsePacket(isYou: Boolean, valid: Boolean, newCard: CardState?, newRam: Int): SummonPacket {
        return SummonPacket(isYou, valid, position, newCard, newRam)
    }

}

@Serializable
@SerialName(PacketType.SUMMON)
@Suppress("PropertyName")
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
