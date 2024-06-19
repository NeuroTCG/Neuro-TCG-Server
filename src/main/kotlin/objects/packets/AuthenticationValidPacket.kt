package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
@SerialName(PacketType.AUTHENTICATION_VALID)
class AuthenticationValidPacket(
    @Required val has_running_game: Boolean,
    @Required val you: UserInfo
) : Packet() {}
