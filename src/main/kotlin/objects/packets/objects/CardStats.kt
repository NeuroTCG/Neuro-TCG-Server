package objects.packets.objects

import kotlinx.serialization.*
import objects.shared.*

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

/** The stats, name, etc. for all instances of one card. IDs are not given out in order. */
@Serializable
@Suppress("PropertyName")
class CardStats(
    @Required val name: String,
    @Required var graphics: String?,
    @Required val max_hp: Int,
    @Required val base_atk: Int,
    @Required var summoning_cost: Int,
    @Required val attack_range: AttackRange,
    @Required val card_type: CardType,
    @Required val ability: Ability,
    @Required val has_summoning_sickness: Boolean,
    @Required val trap_card_stats: TrapCardStats?,
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
        val TRAP_MOTHERS_LOVE_ID = 5

        fun normalizeName(name: String): String {
            // expand when needed
            assert(name.matches("[ a-zA-Z'_]".toRegex()))
            return name.replace(" ", "_").replace("'", "_").lowercase()
        }

        // TODO: maybe move this out of here since it isn't networking related
        val cardIDMapping: HashMap<Int, CardStats> =
            hashMapOf(
                0 to
                    CardStats(
                        "Pirate Evil",
                        null,
                        2,
                        2,
                        2,
                        AttackRange.STANDARD,
                        CardType.CREATURE,
                        Ability(),
                        true,
                        null,
                    ),
                1 to
                    CardStats(
                        "Filipino Boy",
                        null,
                        3,
                        3,
                        5,
                        AttackRange.REACH,
                        CardType.CREATURE,
                        Ability(),
                        true,
                        null,
                    ),
                2 to
                    CardStats(
                        "Angel Neuro",
                        null,
                        24,
                        2,
                        0,
                        AttackRange.STANDARD,
                        CardType.DECK_MASTER,
                        Ability(AbilityEffect.ADD_HP, 3, AbilityRange.ALLY_CARD, 4),
                        true,
                        null,
                    ),
                3 to
                    CardStats(
                        "That streaming site she uses",
                        null,
                        3,
                        4,
                        4,
                        AttackRange.STANDARD,
                        CardType.CREATURE,
                        Ability(AbilityEffect.SEAL, 1, AbilityRange.ENEMY_CARD, 0),
                        true,
                        null,
                    ),
                4 to
                    CardStats(
                        "10 Tins Cans 1 Stream",
                        null,
                        0,
                        0,
                        4,
                        AttackRange.STANDARD,
                        CardType.MAGIC,
                        Ability(
                            AbilityEffect.ATTACK,
                            5,
                            AbilityRange.ENEMY_CARD,
                            0,
                        ),
                        true,
                        null,
                    ),
                TRAP_MOTHERS_LOVE_ID to
                    CardStats(
                        "A Mother's Love",
                        null,
                        0,
                        0,
                        3,
                        AttackRange.STANDARD,
                        CardType.TRAP,
                        Ability(),
                        false,
                        TrapCardStats(Activations.ALLY_KILLED, Effects.A_MOTHERS_LOVE),
                    ),
            )

        fun getCardByID(id: Int): CardStats? = cardIDMapping.get(id)
    }
}
