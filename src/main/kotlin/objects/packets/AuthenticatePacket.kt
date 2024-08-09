package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.AUTHENTICATE)
/**
 * Sent by: Client
 *
 * This packet associates a connection with a user.
 *
 * If authentication fails, a [DisconnectPacket] is sent.
 */
class AuthenticatePacket(@Required val username: String) : Packet()
