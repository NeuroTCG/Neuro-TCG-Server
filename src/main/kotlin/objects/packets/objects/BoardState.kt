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
    val cards = mutableListOf<MutableList<MutableList<CardState?>>>(
        mutableListOf(
            mutableListOf(null, null, null, null),
            mutableListOf(null, null, null),
        ),
        mutableListOf(
            mutableListOf(CardState(0, 70), null, null, null),
            mutableListOf(null, null, null),
        )
    )

    @Required
    val traps = mutableListOf<MutableList<TrapCard?>>(
        mutableListOf(null, null),
        mutableListOf(null, null),
    )
    @Required
    var first_player_active: Boolean = true
}
