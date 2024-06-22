package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.UNKNOWN_PACKET)
class UnknownPacketPacket(
    @Required val message: String,
) : Packet() {}
