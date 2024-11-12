/*
 * Created by BSGMatt on 2024.11.11
 */

package objects

import kotlinx.serialization.*
import objects.packets.objects.*

@Serializable
class UpdateAction (
    @Required val action : String,
    @Required val user: CardPosition,
    @Required val target: CardPosition,
    @Required val amount: Int,
)
