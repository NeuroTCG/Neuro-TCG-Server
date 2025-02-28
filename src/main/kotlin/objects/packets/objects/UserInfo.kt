package objects.packets.objects

import kotlinx.serialization.*
import objects.TcgId

@Serializable
class UserInfo(
    @Required val id: TcgId,
)
