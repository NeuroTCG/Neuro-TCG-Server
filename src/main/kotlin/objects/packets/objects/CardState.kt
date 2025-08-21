package objects.packets.objects

import kotlinx.serialization.*
import objects.packets.*

@Serializable(with = CardTurnPhase.Serializer::class)
enum class CardTurnPhase {
    Done,
    AbilityOnly,
    AttackOnly,
    Action,
    MoveOrAbility,
    MoveOrAction,
    ;

    object Serializer : EnumOrdinalSerializer<CardTurnPhase>("CardTurnPhase", entries.toTypedArray())
}

@Suppress("PropertyName")
@Serializable
data class CardState(
    @Required var id: Int,
    @Required var health: Int,
    @Required var ability_was_used: Boolean,
    @Required var phase: CardTurnPhase,
    @Required var shield: Int,
    @Required var sealed_turns_left: Int,
    @Required var attack_bonus: Int = 0,
    @Required var ability_cost_modifier: Int = 0,
) {
    fun currentAttackValue(): Int = CardStats.getCardByID(id)!!.base_atk + attack_bonus

    fun currentAbilityCost(): Int = CardStats.getCardByID(id)!!.ability.cost - ability_cost_modifier

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + health
        result = 31 * result + ability_was_used.hashCode()
        result = 31 * result + phase.hashCode()
        result = 31 * result + shield
        result = 31 * result + sealed_turns_left
        result = 31 * result + attack_bonus
        result = 31 * result + ability_cost_modifier
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CardState

        if (id != other.id) return false
        if (health != other.health) return false
        if (ability_was_used != other.ability_was_used) return false
        if (phase != other.phase) return false
        if (shield != other.shield) return false
        if (sealed_turns_left != other.sealed_turns_left) return false
        if (attack_bonus != other.attack_bonus) return false
        if (ability_cost_modifier != other.ability_cost_modifier) return false

        return true
    }
}
