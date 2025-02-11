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
    @Required var valueX: Int = 0,
    @Required var valueY: Int = 0,
)



