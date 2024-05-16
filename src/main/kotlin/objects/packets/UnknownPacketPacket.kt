package objects.packets

import kotlinx.serialization.*

@Serializable
class UnknownPacketPacket(
    @Required val response_id: Int?,
) : Packet(PacketType.unknown_packet) {}
