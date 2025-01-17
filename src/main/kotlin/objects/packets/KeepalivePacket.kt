package objects.packets

import kotlinx.serialization.*

/**
 * Sent by: Client
 */
@Serializable
@SerialName(PacketType.KEEPALIVE)
class KeepalivePacket : Packet()
