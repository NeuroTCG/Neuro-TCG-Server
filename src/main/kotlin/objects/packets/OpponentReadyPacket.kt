/*
 * Created by BSGMatt on 2025.1.23
 */

package objects.packets

import kotlinx.serialization.*

@Serializable
@SerialName(PacketType.OPPONENT_READY)
class OpponentReadyPacket : Packet()
