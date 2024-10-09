package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

/**
 * Sent by: Server
 *
 * Informs the client of the rules used for the upcoming match. This is sent directly after the AuthenticationValid packet.
 */
@Serializable
@SerialName(PacketType.RULE_INFO)
@Suppress("PropertyName")
class RuleInfoPacket(
    @Required val card_id_mapping: HashMap<Int, CardStats> = CardStats.cardIDMapping,
) : Packet()
