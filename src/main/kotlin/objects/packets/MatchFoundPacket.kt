package objects.packets

import kotlinx.serialization.*
import java.util.*

@Serializable
class MatchFoundPacket(
    @Required val opponent: UserInfo,
    @Required @Serializable(with = UUIDSerializer::class) val game_id: UUID,
    @Required val is_reconnect: Boolean
) : Packet(PacketType.match_found) {}
