package objects.packets

import kotlinx.serialization.*

@Serializable
class AuthenticationValidPacket(
    @Required val has_running_game: Boolean,
) : Packet(PacketType.authentication_valid) {}
