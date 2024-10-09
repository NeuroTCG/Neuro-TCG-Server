package objects.packets

import kotlinx.serialization.*

/**
 * Sent by: Server
 */
@Serializable
@SerialName(PacketType.START_TURN)
class StartTurnPacket : Packet()

/**
 * Sent by: Server or Client
 */
@Serializable
@SerialName(PacketType.END_TURN)
class EndTurnPacket : Packet()
