package objects.packets

import kotlinx.serialization.*

/**
 * Sent by: Client
 *
 */
@Serializable
@SerialName(PacketType.DEBUG_EVENT)
class DebugEventPacket(
    @Required val event: String,
) : Packet()
