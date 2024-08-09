package objects.packets.objects

import kotlinx.serialization.*

@Serializable
enum class AttackRange {
    STANDARD,
    REACH
}

@Serializable
enum class CardType {
    DECK_MASTER,
    CREATURE,
    MAGIC,
    TRAP,
    TOKEN
}

@Serializable
@Suppress("PropertyName")
/**
 * The stats, name, etc. for all instances of one card. IDs are not given out in order.
 */
class CardStats(
    @Required val graphics: String,
    @Required val max_hp: Int,
    @Required val base_atk: Int,
    @Required val summoning_cost: Int,
    @Required val attack_range: AttackRange,
    @Required val card_type: CardType,
    @Required val ability: Ability
) {
    init{
        assert(summoning_cost in 0..10)
    }
    companion object {
        // TODO: maybe move this out of here since it isn't networking related
        val cardIDMapping: HashMap<Int, CardStats> = hashMapOf(
            Pair(0, CardStats(
                "res://assets/game/cards/pirate_evil.jpg",
                2,
                2,
                2,
                AttackRange.STANDARD,
                CardType.CREATURE,
                Ability()
            )), // Pirate Evil / Neuro
            Pair(1, CardStats(
                "res://assets/game/cards/filipino_boy.png",
                3,
                3,
                5,
                AttackRange.REACH,
                CardType.CREATURE,
                Ability()
            )), // Filipino Boy
            Pair(2, CardStats(
                "res://assets/game/cards/angel_neuro.png",
                24,
                2,
                0,
                AttackRange.STANDARD,
                CardType.DECK_MASTER,
                Ability(AbilityEffect.ADD_HP_TO_ALLY_CARD, 3, AbilityRange.ALLY_CARD, 4)
            ))
        )

        fun getCardByID(id: Int): CardStats {
            return cardIDMapping.getValue(id)
        }
    }
}


