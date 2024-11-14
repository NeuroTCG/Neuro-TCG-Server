/*
 * Created by BSGMatt on 2024.11.13
 */

package objects.packets.objects

import kotlinx.serialization.Serializable

@Serializable
class Card(
    val id: Int,
    var position: CardPosition,
    var state: CardState
) {

}
