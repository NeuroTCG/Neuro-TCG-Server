package objects.packets.objects

import kotlinx.serialization.*

@Serializable
enum class Tactic {
    REACH,
    NIMBLE,
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
    @Required val name: String,
    @Required var graphics: String?,
    @Required var summoning_cost: Int,
    @Required val base_atk: Int,
    @Required val max_hp: Int,
    @Required val tactics: Array<Tactic>,
    @Required val card_type: CardType,
    @Required val ability: Ability,
    @Required val passive: Passive = Passive()
) {
    init {
        assert(summoning_cost in 0..10)

        if (FREE_EVERYTHING) {
            summoning_cost = 0
        }

        if (graphics == null) {
            graphics = "$ASSET_PREFIX/${normalizeName(name)}.png"
        }
    }

    companion object {
        val FREE_EVERYTHING = false
        val ASSET_PREFIX = "res://assets/game/cards"

        fun normalizeName(name: String): String {
            // expand when needed
            assert(name.matches("[ a-zA-Z'_]".toRegex()))
            return name.replace(" ", "_").replace("'", "_").lowercase()
        }

        val cardIDMapping: HashMap<Int, CardStats> =
            hashMapOf(
                0 to
                    CardStats(
                        "Pirate Evil",
                        null,
                        2,
                        2,
                        2,
                        arrayOf<Tactic>(Tactic.REACH),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                1 to
                    CardStats(
                        "Filipino Boy",
                        null,
                        4,
                        2,
                        2,
                        arrayOf<Tactic>(Tactic.REACH),
                        CardType.CREATURE,
                        Ability(),
                        Passive(PassiveEffectType.DRAW_ON_DESTRUCTION),
                    ),
                2 to
                    CardStats(
                        "Angel Neuro",
                        null,
                        0,
                        2,
                        24,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(AbilityEffect.ADD_HP, 3, AbilityRange.ALLY_CARD, 4),
                        Passive(PassiveEffectType.BUFF_ADJACENT, 1, 1)
                    ),
                3 to
                    CardStats(
                        "That streaming site she uses",
                        null,
                        4,
                        4,
                        3,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(AbilityEffect.SEAL, 1, AbilityRange.ENEMY_CARD, 0),
                        Passive()
                    ),
                4 to
                    CardStats(
                        "10 Tins Cans 1 Stream",
                        null,
                        5,
                        0,
                        0,
                        arrayOf<Tactic>(),
                        CardType.MAGIC,
                        Ability(AbilityEffect.ATTACK, 5, AbilityRange.ENEMY_ROW, 0),
                        Passive(),
                    ),
            )

        fun getCardByID(id: Int): CardStats? = cardIDMapping.get(id)
    }
}
