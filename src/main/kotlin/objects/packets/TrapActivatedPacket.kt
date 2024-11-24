package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

/**
 * Sent by: Server
 *
 * Informs the client that a trap has been activated
 */
@Serializable
@SerialName(PacketType.TRAP_ACTIVATED)
@Suppress("PropertyName")
class TrapActivatedPacket(
    @Required val id: Int,
    @Required val is_you: Boolean,
) : Packet()
