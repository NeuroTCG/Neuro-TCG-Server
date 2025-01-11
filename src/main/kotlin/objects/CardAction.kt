/*
 * Created by BSGMatt on 2024.11.11
 */

package objects

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
class CardActionList(
    @Required val card: CardData,
    @Required val actions: Array<CardAction>,
) {
    companion object {
        fun emptyActionList(cardData: CardData) = CardActionList(cardData, arrayOf())

        fun testActionList(
            cardData: CardData,
            playerIdx: Int,
        ) = CardActionList(
            cardData,
            arrayOf(
                CardAction(
                    CardActionNames.TEST,
                    arrayOf(
                        CardActionTarget(playerIdx, cardData.position),
                    ),
                    0,
                ),
            ),
        )

        fun drawCardActionList(
            cardData: CardData,
            playerIdx: Int,
            cardId: Int,
        ) = CardActionList(
            cardData,
            arrayOf(CardAction(CardActionNames.DRAW_CARD, arrayOf(CardActionTarget(playerIdx, cardData.position)), cardId)),
        )
    }
}

@Serializable
class CardAction(
    @Required val action_name: String,
    @Required val targets: Array<CardActionTarget>,
    @Required val amount: Int,
    @Required val other_args: Array<CardActionArg> = arrayOf(),
)

@Serializable
class CardActionTarget(
    @Required val playerIdx: Int,
    @Required val position: CardPosition,
)

@Serializable
class CardActionArg(
    @Required val arg_name: String,
    @Required val arg_value: String, // Using a String so that args can be generic
)

object CardActionNames {
    const val TEST = "test"
    const val ADD_HP = "add_hp"
    const val SUB_HP = "sub_hp"
    const val ADD_ATTACK = "add_attack"
    const val SUB_ATTACK = "sub_attack"
    const val ATTACK = "attack"
    const val SET_PHASE = "set_phase"
    const val SET_ABILITY_USED = "set_ability_used"
    const val DRAW_CARD = "draw_card"
}

/**
 * Object housing additional options for CardAction objects.
 */
object CardActionArgs {
    fun minHp(hp: Int): CardActionArg = CardActionArg(CardActionOptionNames.MIN_HP, "$hp")
}

/**
 *
 */
object CardActionOptionNames {
    const val MIN_HP = "min_hp"
}