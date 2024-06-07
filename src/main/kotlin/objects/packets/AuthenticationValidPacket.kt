package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.AUTHENTICATION_VALID)
class AuthenticationValidPacket(
    @Required val has_running_game: Boolean,
) : Packet() {}
