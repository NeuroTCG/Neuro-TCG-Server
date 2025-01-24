/*
 * Created by BSGMatt on 2024.11.15
 */

package objects.packets

import kotlinx.serialization.*
import objects.*

@Serializable
@SerialName(PacketType.PASSIVE_UPDATE)
class PassiveUpdatePacket(
    @Required val updates: Array<CardActionList>,
) : Packet()
