package objects.packets

import kotlinx.serialization.*

@Serializable
class ClientInfoAcceptPacket : Packet(PacketType.client_info_accept) {}
