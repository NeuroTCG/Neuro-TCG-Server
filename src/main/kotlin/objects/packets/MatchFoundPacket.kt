package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.MATCH_FOUND)
@Suppress("PropertyName")
/**
 *
 * Sent by: Server
 *
 * This informs the client that a match was found. It includes information on the opponent. The client and server may now
 * use all gameplay packets. The client can get the game state using the [GetBoardStatePacket] with the reason as
 * [connect][GetBoardStatePacket.Reason.connect] or [reconnect][GetBoardStatePacket.Reason.reconnect].
 *
 * Do **not** generate this state on your own.
 *
 * The `game_id` can be used to spectate the game, if we decide to implement that.
 */
class MatchFoundPacket(
    @Required val opponent: UserInfo,
    //@Required @Serializable(with = UUIDSerializer::class) val game_id: UUID,
    @Required val game_id: Int,
    @Required val is_reconnect: Boolean,
    @Required val is_first_player: Boolean
) : Packet()
