package objects.packets.objects

import kotlinx.serialization.*

@Serializable
data class CardState(@Required var id: Int, @Required var health: Int)
