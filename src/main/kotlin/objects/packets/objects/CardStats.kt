package objects.packets.objects

import kotlinx.serialization.*

@Serializable
enum class AttackRange {
    STANDARD,
    REACH,
}

@Serializable
enum class CardType {
    DECK_MASTER,
    CREATURE,
    MAGIC,
    TRAP,
    TOKEN,
}

/**
 * The stats, name, etc. for all instances of one card. IDs are not given out in order.
 */
@Serializable
@Suppress("PropertyName")
class CardStats(
    @Required val graphics: String,
    @Required val max_hp: Int,
    @Required val base_atk: Int,
    @Required val summoning_cost: Int,
    @Required val attack_range: AttackRange,
    @Required val card_type: CardType,
    @Required val ability: Ability,
    @Required val has_summoning_sickness: Boolean,
) {
    init {
        assert(summoning_cost in 0..10)
    }

    companion object {
        // TODO: maybe move this out of here since it isn't networking related
        val cardIDMapping: HashMap<Int, CardStats> =
            hashMapOf(
                0 to
                    CardStats(
                        "res://assets/game/cards/pirate_evil.jpg",
                        2,
                        2,
                        2,
                        AttackRange.STANDARD,
                        CardType.CREATURE,
                        Ability(),
                        true,
                    ),
                // Pirate Evil / Neuro
                1 to
                    CardStats(
                        "res://assets/game/cards/filipino_boy.png",
                        3,
                        3,
                        5,
                        AttackRange.REACH,
                        CardType.CREATURE,
                        Ability(),
                        true,
                    ),
                // Filipino Boy
                2 to
                    CardStats(
                        "res://assets/game/cards/angel_neuro.png",
                        24,
                        2,
                        0,
                        AttackRange.STANDARD,
                        CardType.DECK_MASTER,
                        Ability(AbilityEffect.ADD_HP, 3, AbilityRange.ALLY_CARD, 4),
                        true,
                    ),
                // Twitch
                3 to
                    CardStats(
                        "res://assets/game/cards/the_streaming_site_she_uses.png",
                        3,
                        4,
                        4,
                        AttackRange.STANDARD,
                        CardType.CREATURE,
                        Ability(AbilityEffect.SEAL, 1, AbilityRange.ENEMY_CARD, 0),
                        true,
                    ),
            )

        fun getCardByID(id: Int): CardStats = cardIDMapping.getValue(id)
    }
}
