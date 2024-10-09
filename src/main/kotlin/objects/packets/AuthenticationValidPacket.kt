package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

/**
 *
 * Sent by: Server
 *
 * Authentication was successful. The client now waits for a [MatchFoundPacket]. The client should inform the user that
 * matchmaking is happening.
 *
 * @param has_running_game a game is still running and the [MatchFoundPacket] will be an existing game. This information is duplicated in the [MatchFoundPacket]. (this is so the client can display "reconnecting..." instead of "waiting for opponent...")
 */
@Serializable
@SerialName(PacketType.AUTHENTICATION_VALID)
@Suppress("PropertyName")
class AuthenticationValidPacket(
    @Required val has_running_game: Boolean,
    @Required val you: UserInfo,
) : Packet()
