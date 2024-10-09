package objects.packets

import kotlinx.serialization.*

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
    @Required val username: String,
) : Packet()
