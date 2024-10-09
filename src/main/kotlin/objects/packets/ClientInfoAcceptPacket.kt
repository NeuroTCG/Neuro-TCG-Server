package objects.packets

import kotlinx.serialization.*

/**
 * Sent by: Server
 *
 * The server has received and accepted the client info. It is now waiting for an [AuthenticatePacket].
 */
@Serializable
@SerialName(PacketType.CLIENT_INFO_ACCEPT)
class ClientInfoAcceptPacket : Packet()
