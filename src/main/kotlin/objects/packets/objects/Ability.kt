package objects.packets.objects

import kotlinx.serialization.*

@Serializable
enum class AbilityEffect {
    NONE,
    ADD_HP_TO_ALLY_CARD,
    SEAL_ENEMY_CARD,
    ATTACK,
    ATTACK_ROW
}

@Serializable
enum class AbilityRange {
    NONE,
    ALLY_FIELD,
    ENEMY_FIELD,
    ALLY_CARD,
    ENEMY_CARD,
    ENEMY_ROW,
    ALL_ENEMY_CARDS,
    ALL_ALLY_CARDS,
    PLAYER_DECK
}

@Serializable
data class Ability(
    @Required var effect: AbilityEffect = AbilityEffect.NONE,
    @Required var value: Int = 0,
    @Required val range: AbilityRange = AbilityRange.NONE,
    @Required val cost: Int = 0
)
