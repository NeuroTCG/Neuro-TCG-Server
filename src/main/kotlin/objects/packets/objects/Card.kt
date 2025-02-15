/*
 * Created by BSGMatt on 2024.11.13
 */

package objects.packets.objects

import kotlinx.serialization.Serializable

/**
 * A Container for the position and state of a card.
 */
@Serializable
class Card(
    // 0 for player1 1 for player 2
    var playerIdx: Int,
    var position: CardPosition,
    var state: CardState,
)
