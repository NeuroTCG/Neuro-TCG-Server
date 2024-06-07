package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.CLIENT_INFO)
class ClientInfoPacket(
    @Required val client_name: String,
    @Required val client_version: String,
    @Required val protocol_version: Int
) : Packet() {
    init {
        require(client_name.length <= 25)
        require(client_version.length <= 40)
    }
}
