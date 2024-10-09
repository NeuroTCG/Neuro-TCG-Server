package objects.packets

import kotlinx.serialization.*

/**
 * Sent by: Both
 *
 * This informs the other party that an unknown packet was received. If a client receives this, it is either out of date or
 * has a bug.
 *
 * The client may send this packet to the server, but if a client doesn't understand a packet, it is likely their fault.
 * Sending this packet to the server will result in the connection being closed. Use this opportunity to inform the user of
 * this, and they can maybe continue the game with a newer version or the official client. Don't try to ignore an
 * unknown packet, as it will most likely result in state conflicts later and will just make for a bad user experience.
 *
 * This is the only packet that is valid in both the init phase and the gameplay phase.
 */
@Serializable
@SerialName(PacketType.UNKNOWN_PACKET)
class UnknownPacketPacket(
    @Required val message: String,
) : Packet()
