package objects.packets

import kotlinx.serialization.*
import objects.packets.objects.*

/**
 * Sent by: Server
 *
 * Tells the client who has won / lost the game
 */
@Serializable
@SerialName(PacketType.GAME_OVER)
@Suppress("PropertyName")
class GameOverPacket(
    @Required val you_are_winner: Boolean,
) : Packet()
