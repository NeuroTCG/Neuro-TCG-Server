package objects.packets.objects

import kotlinx.serialization.*

@Serializable
enum class PassiveEffectType {
    NONE,
    DRAW_ON_DESTRUCTION,
    BUFF_ADJACENT,
    CARD_DISCOUNT,
    REACH_HP_THRESHOLD,
    ATTACK_AFTER_ABILITY,
}

@Serializable
data class Passive(
    @Required val effect: PassiveEffectType = PassiveEffectType.NONE,
    @Required val values: IntArray = IntArray(0),
) {
    init {
        require(
            values.size ==
                when (effect) {
                    PassiveEffectType.DRAW_ON_DESTRUCTION -> 0
                    PassiveEffectType.BUFF_ADJACENT -> 2
                    PassiveEffectType.NONE -> 0
                    PassiveEffectType.CARD_DISCOUNT -> 3
                    PassiveEffectType.REACH_HP_THRESHOLD -> 3 // Threshold, +HP, +ATTACK
                    PassiveEffectType.ATTACK_AFTER_ABILITY -> 0
                },
        )
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
