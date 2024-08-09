package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.SUMMON_REQUEST)
@Suppress("PropertyName")
/**
 * Sent by: Client
 *
 * Tries to summon a card at a given position.
 */
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
/**
 *
 * Sent by: Server
 *
 * Informs the client of a summon by either it or the opponent.
 *
 * @param is_you True if the player receiving this packet is the player who caused it
 */
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
