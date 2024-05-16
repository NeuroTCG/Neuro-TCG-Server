package objects.packets

import kotlinx.serialization.*

@Serializable
class AuthenticatePacket(@Required val username: String) : Packet(PacketType.authenticate) {}
