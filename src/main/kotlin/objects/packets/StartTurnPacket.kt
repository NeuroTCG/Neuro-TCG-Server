package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.START_TURN)
/**
 * Sent by: Server
 */
class StartTurnPacket : Packet()

@Serializable
@SerialName(PacketType.END_TURN)
/**
 * Sent by: Server or Client
 */
class EndTurnPacket : Packet()
