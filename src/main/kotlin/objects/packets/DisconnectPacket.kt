package objects.packets

import kotlinx.serialization.*

@Serializable
class DisconnectPacket(
    @Required val reason: Reason,
    @Required val message: String,
) : Packet(PacketType.disconnect) {
    @Serializable
    enum class Reason {
        auth_invalid,
        auth_user_banned,
        protocol_too_old,
    }
}
