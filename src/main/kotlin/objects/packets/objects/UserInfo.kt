package objects.packets.objects

import kotlinx.serialization.*

@Serializable
class UserInfo(
    @Required val username: String,
    @Required val region: String,
)
