package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.CLIENT_INFO_ACCEPT)
class ClientInfoAcceptPacket : Packet() {}
