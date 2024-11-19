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
)

@Serializable
class CardAction(
    @Required val action_name: String,
    @Required val targets: Array<CardActionTarget>,
    @Required val amount: Int,
)

@Serializable
class CardActionTarget(
    @Required val playerIdx: Int,
    @Required val position: CardPosition,
)

object CardActionNames {
    const val TEST = "test"
    const val ADD_HP = "add_hp"
    const val ATTACK = "attack"
    const val HEAL = "heal"
    const val SET_PHASE = "set_phase"
    const val SET_ABILITY_USED = "set_ability_used"
}
