/*
 * Created by BSGMatt on 2025.1.21
 */

package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.GAME_START)
class GameStartPacket : Packet()
