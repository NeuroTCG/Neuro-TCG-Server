package objects.packets.objects

import kotlinx.serialization.*
import objects.packets.objects.CardType

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
    @Required val passive: Passive = Passive(),
) {
    init {
        assert(summoning_cost in 0..10)

        if (FREE_EVERYTHING) {
            summoning_cost = 0
        }

        if (graphics == null) {
            graphics = "$ASSET_PREFIX/${normalizeName(name)}.png"
        }

        if (card_type == CardType.DECK_MASTER) {
            assert(summoning_cost == 0)
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

        var maxID = 0

        val cardIDMapping: HashMap<Int, CardStats> =
            hashMapOf(
                maxID++ to
                    CardStats(
                        "Neuro",
                        null,
                        0,
                        3,
                        27,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.NONE, // revive creature
                            0,
                            AbilityRange.ALLY_FIELD,
                            5,
                        ),
                        Passive(
                            PassiveEffectType.NONE, // can move after attacking
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Evil Neuro",
                        null,
                        0,
                        3,
                        22,
                        arrayOf<Tactic>(Tactic.REACH),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.NONE, // destroy random creature
                            0,
                            AbilityRange.ENEMY_FIELD,
                            5,
                        ),
                        Passive(
                            PassiveEffectType.NONE,
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Angel Neuro",
                        null,
                        0,
                        2,
                        24,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.ADD_HP,
                            3,
                            AbilityRange.ALLY_CARD,
                            4,
                        ),
                        Passive(
                            PassiveEffectType.BUFF_ADJACENT,
                            intArrayOf(1, 1),
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Study-Sama",
                        null,
                        0,
                        3,
                        28,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.NONE, // ADD_ATK
                            2,
                            AbilityRange.ALLY_FIELD,
                            6,
                        ),
                        Passive(
                            PassiveEffectType.NONE,
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Karaoke Neuro", // sheet also mentions evil version
                        null,
                        0,
                        2,
                        22,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.SEAL,
                            2,
                            AbilityRange.ENEMY_CARD,
                            4,
                        ),
                        Passive(
                            PassiveEffectType.NONE,
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "The Swarm",
                        null,
                        0,
                        3,
                        27,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.ATTACK,
                            1,
                            AbilityRange.ENEMY_ROW,
                            4,
                        ),
                        Passive(
                            PassiveEffectType.NONE, // gains attack for every card it destroys;
                            // maybe implement together with hp gain per attack
                            intArrayOf(1), // amount gained
                        ),
                    ),
                maxID++ to
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
                maxID++ to
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
                maxID++ to
                    CardStats(
                        "That streaming site she uses",
                        null,
                        4,
                        4,
                        3,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(AbilityEffect.SEAL, 1, AbilityRange.ENEMY_CARD, 0),
                        Passive(),
                    ),
                maxID++ to
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
