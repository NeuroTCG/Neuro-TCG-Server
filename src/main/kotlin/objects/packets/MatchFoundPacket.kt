package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.MATCH_FOUND)
@Suppress("PropertyName")
class MatchFoundPacket(
    @Required val opponent: UserInfo,
    //@Required @Serializable(with = UUIDSerializer::class) val game_id: UUID,
    @Required val game_id: Int,
    @Required val is_reconnect: Boolean,
    @Required val is_first_player: Boolean
) : Packet()
