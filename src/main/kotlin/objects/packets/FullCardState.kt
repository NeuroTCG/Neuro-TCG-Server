package objects.packets

import kotlinx.serialization.*

@Serializable
data class FullCardState(@Required var id: Int, @Required var health: Int)
