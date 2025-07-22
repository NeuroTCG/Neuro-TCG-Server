package objects.packets

import kotlinx.serialization.*
import objects.Token

/**
 * Sent by: Client
 *
 * This packet associates a connection with a user.
 *
 * If authentication fails, a [DisconnectPacket] is sent.
 */
@Serializable
@SerialName(PacketType.AUTHENTICATE)
class AuthenticatePacket(
    @Required val response_id: Int,
    @Required val token: Token,
) : Packet()
