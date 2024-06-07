package objects.shared

import kotlinx.serialization.*
import objects.packets.*

@Serializable
class BoardState {
    // Initial Board State:
    // [
    // [[0,0,0,0],[0,0,0],[0,0]]
    // [[0,0,0,0],[0,0,0],[0,0]]
    // ]
    @Required
    val rows = mutableListOf<MutableList<MutableList<FullCardState?>>>(
        mutableListOf(
            mutableListOf(null, null, null, null),
            mutableListOf(null, null, null),
        ),
        mutableListOf(
            mutableListOf(FullCardState(0, 70), null, null, null),
            mutableListOf(null, null, null),
        )
    )

    @Required
    val traps = mutableListOf<MutableList<TrapCard?>>(
        mutableListOf(null, null),
        mutableListOf(null, null),
    )
}
