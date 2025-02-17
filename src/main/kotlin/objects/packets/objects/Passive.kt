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
    @Required var valuesSize: Int = 0
) {
    init {
        require(values.size == valuesSize)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Passive

        if (effect != other.effect) return false
        if (!values.contentEquals(other.values)) return false
        if (valuesSize != other.valuesSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = effect.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + valuesSize
        return result
    }
}
