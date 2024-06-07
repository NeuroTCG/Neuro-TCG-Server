package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.AUTHENTICATE)
class AuthenticatePacket(@Required val username: String) : Packet() {}
