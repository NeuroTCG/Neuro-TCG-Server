package objects.packets

import kotlinx.serialization.*

@Serializable
class ClientInfoPacket(
    @Required val client_name: String,
    @Required val client_version: String,
    @Required val protocol_version: ULong
) : Packet(PacketType.client_info) {
    init{
        require(client_name.length <= 25)
        require(client_version.length <= 40)
    }
}
