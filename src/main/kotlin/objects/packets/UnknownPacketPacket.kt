package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.UNKNOWN_PACKET)
class UnknownPacketPacket(
    @Required val response_id: Int?,
) : Packet() {}
