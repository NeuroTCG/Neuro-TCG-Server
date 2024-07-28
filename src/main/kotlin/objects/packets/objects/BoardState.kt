package objects.packets.objects

import kotlinx.serialization.*
import objects.shared.*

@Serializable
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
    var ram = arrayOf<Int>(1, 1)

    var max_ram = arrayOf<Int>(1, 1)
}
