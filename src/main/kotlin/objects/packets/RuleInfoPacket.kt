package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.RULE_INFO)
class RuleInfoPacket(
    @Required val card_id_mapping: HashMap<Int, CardStats> = CardStats.cardIDMapping
) : Packet() {}
