/*
 * Created by BSGMatt on 2025.1.26
 */

package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.PLAYER_READY)
class PlayerReadyPacket : Packet()
