package objects.packets.objects

import kotlinx.serialization.*

@Serializable
enum class AbilityEffect {
    NONE,
    ADD_HP,
    SEAL,
    ATTACK,
}

@Serializable
enum class AbilityRange {
    NONE,
    ALLY_FIELD,
    ENEMY_FIELD,
    ALLY_CARD,
    ENEMY_CARD,
    ENEMY_ROW,
    PLAYER_DECK
}

@Serializable
data class Ability(
    @Required var effect: AbilityEffect = AbilityEffect.NONE,
    @Required var value: Int = 0,
    @Required val range: AbilityRange = AbilityRange.NONE,
    @Required val cost: Int = 0
)
