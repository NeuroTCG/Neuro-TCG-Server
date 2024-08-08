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
        arrayOf(
            arrayOfNulls(4),
            arrayOfNulls(3),
        ), arrayOf(
            arrayOfNulls(4),
            arrayOfNulls(3),
        )
    )

    @Required
    val traps = arrayOf<Array<TrapCard?>>(
        arrayOfNulls(2),
        arrayOfNulls(2),
    )

    @Required
    var first_player_active: Boolean = true

    @Required
    var hands = arrayOf<MutableList<Int>>(mutableListOf(), mutableListOf())

    @Required
    var ram = arrayOf(1, 1)

    @Required
    var max_ram = arrayOf(1, 1)
}
