/*
 * Created by BSGMatt on 2024.11.11
 */

package objects

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
class CardActionList(
    @Required val user: Card,
    @Required val actions: Array<CardAction>,
)

@Serializable class CardAction(
    @Required val action_name: String,
    @Required val targets: Array<Card>,
    @Required val amount: Int,
)

object CardActionNames {
    const val TEST = "test"
    const val ADD_HP = "add_hp"
    const val ADD_ATTACK = "add_attack"
    const val SET_PHASE = "set_phase"
}
