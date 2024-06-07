package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.DISCONNECT)
class DisconnectPacket(
    @Required val reason: Reason,
    @Required val message: String,
) : Packet() {
    @Serializable
    enum class Reason {
        auth_invalid,
        auth_user_banned,
        protocol_too_old,
    }
}
