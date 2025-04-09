package objects.packets.objects

import kotlinx.serialization.*
import objects.*
import objects.packets.objects.PassiveEffectType
import objects.passives.*

@Serializable
enum class PassiveEffectType {
    NONE,
    NOT_IMPLEMENTED,
    DRAW_ON_DESTRUCTION, // TODO: make num cards configurable
    BUFF_ADJACENT, // atk, hp
}

@Serializable
data class Passive(
    @Required val effect: PassiveEffectType = PassiveEffectType.NONE,
    @Required val values: IntArray = IntArray(0),
) {
    init {
        if (effect != PassiveEffectType.NOT_IMPLEMENTED) {
            require(
                values.size ==
                    when (effect) {
                        PassiveEffectType.DRAW_ON_DESTRUCTION -> 1
                        PassiveEffectType.BUFF_ADJACENT -> 2
                        PassiveEffectType.NONE -> 0
                        PassiveEffectType.NOT_IMPLEMENTED -> require(false)
                    },
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Passive

        if (effect != other.effect) return false
        if (!values.contentEquals(other.values)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = effect.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}
