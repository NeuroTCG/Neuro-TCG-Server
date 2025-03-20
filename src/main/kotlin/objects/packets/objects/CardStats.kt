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
        require(summoning_cost in 0..10) { "A card must cost 0-10 ram to summon" }

        if (FREE_EVERYTHING) {
            summoning_cost = 0
        }

        if (graphics == null) {
            graphics = "$ASSET_PREFIX/${normalizeName(name)}.png"
        }

        if (card_type == CardType.DECK_MASTER) {
            require(summoning_cost == 0) { "deck masters must always have a cost of 0" }
        }
    }

    companion object {
        val FREE_EVERYTHING = false
        val ASSET_PREFIX = "res://assets/game/cards"

        fun normalizeName(name: String): String {
            // expand when needed
            require(name.matches("[ a-zA-Z0-9'_\\-]+".toRegex())) { "'$name' contains unhandled characters" }
            return name
                .replace(" ", "_")
                .replace("'", "_")
                .replace("-", "_")
                .lowercase()
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
                            AbilityRange.NONE, // ENEMY_CREATURE
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
                    CardStats( // maybe give ID 987 as an easter egg
                        "Vedal",
                        null,
                        0,
                        1,
                        25,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.ATTACK,
                            2,
                            AbilityRange.ENEMY_FIELD,
                            8,
                        ),
                        Passive(
                            PassiveEffectType.BUFF_ADJACENT,
                            intArrayOf(1, 0), // amount gained
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Cerber",
                        null,
                        0,
                        2,
                        24,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.NONE, // REMOVE_CARD_AND_GAIN_ATK_HP
                            1, // this has two parameters technically: atk and hp, both 1 here
                            AbilityRange.ALLY_CARD,
                            2,
                        ),
                        Passive(
                            PassiveEffectType.NONE, // can (or has to?) attack after using its ability
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Filian",
                        null,
                        0,
                        2,
                        22,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.NONE, // DRAW_CARD
                            1, // number of cards drawn
                            AbilityRange.PLAYER_DECK, // not really relevant, but it's the closest we have
                            4,
                        ),
                        Passive(
                            PassiveEffectType.NONE, // magic cards are cheaper
                            intArrayOf(1, 1), // reduction, minimum
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Anny",
                        null,
                        0,
                        2,
                        28,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.NONE, // ADD_HP_AND_ATK
                            1, // card gains 1 atk and 1 hp, but they should be independently configurable
                            AbilityRange.ALLY_CARD,
                            3,
                        ),
                        Passive(
                            PassiveEffectType.NONE, // BUFF_ALL_ALLIES, but only after reaching 12 hp for the first time
                            intArrayOf(2, 2), // atk, hp
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Koko",
                        null,
                        0,
                        3,
                        28,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.NONE, // return card to default state (unsure if hp too); doesn't affect ability
                            0, // no parameter
                            AbilityRange.NONE, // ENEMY_CREATURES
                            4,
                        ),
                        Passive(
                            PassiveEffectType.NONE, // COUNTER_ATTACK_BUFF
                            intArrayOf(1), // additional damage for counterattack
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Tenma",
                        null,
                        0,
                        2,
                        24,
                        arrayOf<Tactic>(),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.NONE, // REDUCE_ATK
                            2, // reduction; either second param for minimum or implicitly use 1
                            AbilityRange.ENEMY_CARD,
                            5,
                        ),
                        Passive(
                            PassiveEffectType.NONE, // ACT_MULTIPLE_TIMES
                            intArrayOf(1), // number of additional times; could also store n+1
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Mini",
                        null,
                        0,
                        0, // still does 2 dmg for counterattack and cannot gain atk
                        32,
                        arrayOf<Tactic>(Tactic.REACH),
                        CardType.DECK_MASTER,
                        Ability(
                            AbilityEffect.SHIELD,
                            2,
                            AbilityRange.ALLY_CARD,
                            6,
                        ),
                        Passive(
                            PassiveEffectType.NONE, // the special atk property above
                            intArrayOf(2), // dmg for counterattack
                        ),
                    ),
                // ------------------------ Creatures ------------------------ //
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
                        Passive(
                            PassiveEffectType.DRAW_ON_DESTRUCTION,
                            intArrayOf(1), // num cards to draw
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Judge Vedal",
                        null,
                        4,
                        2,
                        4,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Neuro Bread",
                        null,
                        3,
                        2,
                        2,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NONE, // CONSUMABLE
                            intArrayOf(2, 2), // atk, hp gained when a card moves onto this one
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Astronaut Neuro",
                        null,
                        5,
                        3,
                        3,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NONE, // BOUNCE_AND_GAIN_HP_IN_CORNER; hp only stays for the opponents turn
                            intArrayOf(2), // hp gained in corner
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "The Time God",
                        null,
                        4,
                        2,
                        1,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NONE, // PASS_TURN; turn counters (like seal) are reduced for both players
                            intArrayOf(1), // num turns to pass
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Roaster Neuro",
                        null,
                        5,
                        5,
                        2,
                        arrayOf<Tactic>(Tactic.REACH),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Harrison Temple",
                        null,
                        4,
                        2,
                        2,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NONE, // buff if swaps with the deckmaster as first thing in turn
                            intArrayOf(1, 0), // atk, hp
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Fisherman Neuro",
                        null,
                        2,
                        1,
                        1,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NONE, // ON_DESTROY CHOOSE_AND_SHUFFLE_CARD
                            intArrayOf(3, 1), // num cards to look at, num cards to pick
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Rizzdal",
                        null,
                        4,
                        2,
                        2,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NONE, // BUFF_IF_ALONE
                            intArrayOf(2, 2), // atk, hp
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Ermfish",
                        null,
                        1,
                        1,
                        1,
                        arrayOf<Tactic>(Tactic.NIMBLE),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Ermshark",
                        null,
                        6,
                        5,
                        5,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NONE, // BUFF_ON_DESTRUCTION; only destructions caused by this card
                            intArrayOf(1, 0), // atk, hp
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Teacher Cerber",
                        null,
                        6,
                        3,
                        4,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NONE, // BUFF_ADJACIENT_AT_END_OF_TURN
                            intArrayOf(1, 0), // atk, hp
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
