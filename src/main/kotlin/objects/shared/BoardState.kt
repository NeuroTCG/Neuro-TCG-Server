package objects.shared

import kotlinx.serialization.*

@Serializable
class BoardState{
    // Initial Board State:
    // [
    // [[0,0,0,0],[0,0,0],[0,0]]
    // [[0,0,0,0],[0,0,0],[0,0]]
    // ]
    @Required
    val rows = mutableListOf<MutableList<MutableList<PlayedCard?>>>(
        mutableListOf<MutableList<PlayedCard?>>(
            mutableListOf<PlayedCard?>(null, null, null, null),
            mutableListOf<PlayedCard?>(null, null, null),
        ),
        mutableListOf<MutableList<PlayedCard?>>(
            mutableListOf<PlayedCard?>(PlayedCard(0), null, null, null),
            mutableListOf<PlayedCard?>(null, null, null),
        )
    )
    @Required
    val traps = mutableListOf<MutableList<TrapCard?>>(
        mutableListOf<TrapCard?>(null, null),
        mutableListOf<TrapCard?>(null, null),
    )
}
