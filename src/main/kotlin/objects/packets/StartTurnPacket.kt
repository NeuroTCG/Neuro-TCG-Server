package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.START_TURN)
class StartTurnPacket : Packet() {}

@Serializable
@SerialName(PacketType.END_TURN)
class EndTurnPacket : Packet()
