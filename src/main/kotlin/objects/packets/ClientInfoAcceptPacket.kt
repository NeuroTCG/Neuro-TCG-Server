package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.CLIENT_INFO_ACCEPT)
/**
 * Sent by: Server
 *
 * The server has received and accepted the client info. It is now waiting for an [AuthenticatePacket].
 */
class ClientInfoAcceptPacket : Packet()
