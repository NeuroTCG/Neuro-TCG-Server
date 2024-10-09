package objects.packets

import kotlinx.serialization.*

/**
 * Sent by: Client
 *
 * This is the first packet that is sent for any connection to `/game`.
 *
 * If the protocol version doesn't match the server, a [DisconnectPacket] is sent.
 *
 * @param client_name Can be up to 15 characters long (only used for statistics)
 * @param client_version Can be up to 40 characters long (only used for statistics)
 * @param protocol_version the latest version that the client supports
 */
@Serializable
@SerialName(PacketType.CLIENT_INFO)
@Suppress("PropertyName")
class ClientInfoPacket(
    @Required val client_name: String,
    @Required val client_version: String,
    @Required val protocol_version: Int,
) : Packet() {
    init {
        require(client_name.length <= 25)
        require(client_version.length <= 40)
    }
}
