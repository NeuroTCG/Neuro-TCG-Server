package objects.packets.objects

import kotlinx.serialization.*
import objects.*
import objects.passives.*

@Serializable
enum class PassiveEffectType {
    NONE,
    DRAW_ON_DESTRUCTION,
    BUFF_ADJACENT,
}

@Serializable
data class Passive(
    @Required var effect: PassiveEffectType = PassiveEffectType.NONE,
    @Required var values: IntArray = IntArray(0),
    @Required var values_size: Int = 0
) {
    init {
        require(values.size == values_size)
    }
}
