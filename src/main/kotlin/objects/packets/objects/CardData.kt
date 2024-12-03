/*
 * Created by BSGMatt on 2024.11.13
 */

package objects.packets.objects

import kotlinx.serialization.Serializable

/**
 * A Container for the position and state of a card.
 */
@Serializable
class CardData(
    var playerIdx: Int, // 0 for player1 1 for player 2
    var position: CardPosition,
    var state: CardState,
)
