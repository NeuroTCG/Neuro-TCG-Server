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
                            AbilityEffect.NOT_IMPLEMENTED, // revive creature
                            0,
                            AbilityRange.ALLY_FIELD,
                            5,
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // can move after attacking
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
                            AbilityEffect.NOT_IMPLEMENTED, // destroy random creature
                            0,
                            AbilityRange.NONE, // ENEMY_CREATURE
                            5,
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED,
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
                            AbilityEffect.NOT_IMPLEMENTED, // ADD_ATK
                            2,
                            AbilityRange.ALLY_FIELD,
                            6,
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED,
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
                            PassiveEffectType.NOT_IMPLEMENTED,
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
                            PassiveEffectType.NOT_IMPLEMENTED, // gains attack for every card it destroys;
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
                            AbilityEffect.NOT_IMPLEMENTED, // REMOVE_CARD_AND_GAIN_ATK_HP
                            1, // this has two parameters technically: atk and hp, both 1 here
                            AbilityRange.ALLY_CARD,
                            2,
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // can (or has to?) attack after using its ability
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
                            AbilityEffect.NOT_IMPLEMENTED, // DRAW_CARD
                            1, // number of cards drawn
                            AbilityRange.PLAYER_DECK, // not really relevant, but it's the closest we have
                            4,
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // magic cards are cheaper
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
                            AbilityEffect.NOT_IMPLEMENTED, // ADD_HP_AND_ATK
                            1, // card gains 1 atk and 1 hp, but they should be independently configurable
                            AbilityRange.ALLY_CARD,
                            3,
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // BUFF_ALL_ALLIES, but only after reaching 12 hp for the first time
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
                            AbilityEffect.NOT_IMPLEMENTED, // return card to default state (unsure if hp too); doesn't affect ability
                            0, // no parameter
                            AbilityRange.NONE, // ENEMY_CREATURES
                            4,
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // COUNTER_ATTACK_BUFF
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
                            AbilityEffect.NOT_IMPLEMENTED, // REDUCE_ATK
                            2, // reduction; either second param for minimum or implicitly use 1
                            AbilityRange.ENEMY_CARD,
                            5,
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // ACT_MULTIPLE_TIMES
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
                            1,
                            AbilityRange.ALLY_CARD,
                            6,
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // the special atk property above
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
                            PassiveEffectType.NOT_IMPLEMENTED, // CONSUMABLE
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
                            PassiveEffectType.NOT_IMPLEMENTED, // BOUNCE_AND_GAIN_HP_IN_CORNER; hp only stays for the opponents turn
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
                            PassiveEffectType.NOT_IMPLEMENTED, // PASS_TURN; turn counters (like seal) are reduced for both players
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
                            PassiveEffectType.NOT_IMPLEMENTED, // buff if swaps with the deckmaster as first thing in turn
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
                            PassiveEffectType.NOT_IMPLEMENTED, // ON_DESTROY CHOOSE_AND_SHUFFLE_CARD
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
                            PassiveEffectType.NOT_IMPLEMENTED, // BUFF_IF_ALONE
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
                            PassiveEffectType.NOT_IMPLEMENTED, // BUFF_ON_DESTRUCTION; only destructions caused by this card
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
                            PassiveEffectType.NOT_IMPLEMENTED, // BUFF_ADJACIENT_AT_END_OF_TURN
                            intArrayOf(1, 0), // atk, hp
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Void Sama",
                        null,
                        1,
                        1,
                        1,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.DRAW_ON_DESTRUCTION,
                            intArrayOf(1), // num cards
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Architect Neuro",
                        null,
                        1,
                        1,
                        2,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.NOT_IMPLEMENTED, // SPAWN_CARD
                            -1, // card id to spawn; will be the id for house token
                            AbilityRange.NONE, // RANDOM_FRONT_ROW_ALLY
                            0, // cost unknown
                        ),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "The Baker Twins",
                        null,
                        2,
                        1,
                        3,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.ADD_HP,
                            2,
                            AbilityRange.ALLY_CARD,
                            0, // cost unknown
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // ACT_MULTIPLE_TIMES
                            intArrayOf(1), // number of additional times; could also store n+1
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Grenedier Evil",
                        null,
                        3,
                        2,
                        3,
                        arrayOf<Tactic>(Tactic.REACH),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.ATTACK,
                            2,
                            AbilityRange.ENEMY_ROW,
                            0, // cost unknown
                        ),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "ASMR Evil",
                        null,
                        4,
                        2,
                        3,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Corpa Vedal",
                        null,
                        7,
                        6,
                        8,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Chat",
                        null,
                        8,
                        4,
                        6,
                        arrayOf<Tactic>(Tactic.NIMBLE),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.SEAL,
                            2,
                            AbilityRange.ENEMY_CARD,
                            0, // cost unknown
                        ),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Bartender Neuro",
                        null,
                        3,
                        2,
                        3,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.ADD_HP,
                            3,
                            AbilityRange.ALLY_CARD,
                            0, // cost unknown
                        ),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Neuro Archiver",
                        null,
                        7,
                        2,
                        2,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // RESPAWN_RANDOM_CARD_ON_SUMMON
                            intArrayOf(),
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "2020 Dodge Charger",
                        null,
                        1,
                        2,
                        2,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Rum Drunk Vedal",
                        null,
                        3,
                        1,
                        3,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.ATTACK,
                            1,
                            AbilityRange.ENEMY_ROW,
                            0, // cost unknown
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // ATTACK_CAUSES_MEMORYLEAK
                            intArrayOf(),
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Pirate Neuro", // sheet also mentions evil version                        null,
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
                        "Femboy Vedal",
                        null,
                        7,
                        4,
                        5,
                        arrayOf<Tactic>(Tactic.NIMBLE),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // ATTACK_CAUSES_MEMORYLEAK
                            intArrayOf(),
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Abber Demon",
                        null,
                        6,
                        4,
                        4,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.ATTACK, // direct damage
                            5,
                            AbilityRange.ENEMY_CARD,
                            0, // cost unknown
                        ),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Hacker Vedal",
                        null,
                        4,
                        2,
                        2,
                        arrayOf<Tactic>(Tactic.NIMBLE, Tactic.REACH),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // ATTACK_CAUSES_MEMORYLEAK
                            intArrayOf(),
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Gymbag",
                        null,
                        6,
                        0,
                        8,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.ATTACK,
                            2,
                            AbilityRange.ENEMY_ROW,
                            0, // cost unknown
                        ),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // DEAL_DAMAGE_TO_ALL_ENEMIES_ON_SUMMON, cannot act or counterattack
                            intArrayOf(2), // damage
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "That streaming site she uses", // TM missing
                        null,
                        4,
                        4,
                        3,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.SEAL,
                            1,
                            AbilityRange.ENEMY_CARD,
                            0, // cost unknown
                        ),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Toffee",
                        null,
                        1,
                        2,
                        2,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // BUFF_IF_IS_DECKMASTER
                            intArrayOf(0, 1, 1), // ID of deckmaster (anny here, but unknown for now), atk/hp
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Fanny pack Vedal",
                        null,
                        2,
                        1,
                        1,
                        arrayOf<Tactic>(Tactic.NIMBLE),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // CANT_BE_COUNTERATTACKED
                            intArrayOf(),
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Cowboy Neuro",
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
                        "Hello Chat",
                        null,
                        0, // 2 or 0
                        2,
                        3,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // FREE_IF_SUMMONED_ON_FIRST_TURN
                            intArrayOf(),
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "5 Million Evil Fumos",
                        null,
                        5,
                        5,
                        5,
                        arrayOf<Tactic>(Tactic.REACH),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Neuro Fumo",
                        null,
                        5,
                        2,
                        5,
                        arrayOf<Tactic>(Tactic.NIMBLE),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.ADD_HP,
                            5,
                            AbilityRange.ALLY_CARD, // not sure if ally only
                            0, // cost unknown
                        ),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Ninja Evil",
                        null,
                        2,
                        1,
                        1,
                        arrayOf<Tactic>(Tactic.NIMBLE, Tactic.REACH),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // PIN_ON_ATTACK
                            intArrayOf(1), // num turns
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Cat Neuro",
                        null,
                        0, // unknown
                        0, // unknown
                        0, // unknown
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Axel",
                        null,
                        0, // unknown
                        0, // unknown
                        0, // unknown
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Cerby",
                        null,
                        0, // unknown
                        0, // unknown
                        0, // unknown
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Harpoon Queen",
                        null,
                        5,
                        2,
                        2,
                        arrayOf<Tactic>(Tactic.REACH),
                        CardType.CREATURE,
                        Ability(
                            AbilityEffect.NOT_IMPLEMENTED, // MOVE_TO_FRONT_ROW_AND_DAMAGE
                            1, // damage
                            AbilityRange.ENEMY_CARD,
                            0, // cost unknown
                        ),
                        Passive(),
                    ),
                maxID++ to
                    CardStats(
                        "Minyan",
                        null,
                        2,
                        0,
                        4,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // BUFF_IF_IS_DECKMASTER
                            intArrayOf(0, 0, 1), // ID of deckmaster (mini here, but unknown for now), atk/hp
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "Record Breaking Subathon",
                        null,
                        5,
                        0,
                        3,
                        arrayOf<Tactic>(),
                        CardType.CREATURE,
                        Ability(),
                        Passive(
                            PassiveEffectType.NOT_IMPLEMENTED, // DECKMASTERS_CANT_DIE, can't be buffed, only summonable in first row
                            intArrayOf(1), // minimal HP limit for deckmasters
                        ),
                    ),
                maxID++ to
                    CardStats(
                        "10 Tin Cans 1 Stream",
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
