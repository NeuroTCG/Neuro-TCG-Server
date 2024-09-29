package objects.packets.objects

import kotlinx.serialization.*
import objects.shared.*


@Serializable
@Suppress("PropertyName")
class BoardState {
    // Initial Board State:
    // [
    // [[0,0,0,0],[0,0,0],[0,0]]
    // [[0,0,0,0],[0,0,0],[0,0]]
    // ]
    @Required
    val cards = arrayOf<Array<Array<CardState?>>>(
        // player1
        arrayOf(
            // local top row
            arrayOfNulls(4),
            // local bottom row
            arrayOfNulls(3),
        ),

        // player2
        arrayOf(
            // local top row
            arrayOfNulls(4),
            // local bottom row
            arrayOfNulls(3),
        )
    )

    @Required
    val traps = arrayOf<Array<TrapCard?>>(
        // player1 [local left, local right]
        arrayOfNulls(2),
        // player2 [local left, local right]
        arrayOfNulls(2),
    )

    @Required
    var first_player_active: Boolean = true

    @Required
    var hands = arrayOf<MutableList<Int>>(
        // player1
        mutableListOf(),
        // player2
        mutableListOf(),
    )

    @Required
    var ram = arrayOf(1, 1)

    @Required
    var max_ram = arrayOf(1, 1)

    // TODO: Ideally, the deck master will already be on the board, it
    // shouldn't be possible for these to be null
    @Required
    var deck_masters = arrayOf<CardState?>(null, null)
}
