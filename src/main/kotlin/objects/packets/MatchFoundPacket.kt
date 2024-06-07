package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.MATCH_FOUND)
class MatchFoundPacket(
    @Required val opponent: UserInfo,
    //@Required @Serializable(with = UUIDSerializer::class) val game_id: UUID,
    @Required val game_id: Int,
    @Required val is_reconnect: Boolean
) : Packet() {}
