package objects.shared

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import objects.*
import objects.packets.objects.*
import objects.packets.objects.CardStats.Companion.TRAP_MOTHERS_LOVE_ID

enum class Activations {
    @SerialName("ally_killed")
    ALLY_KILLED,
}

abstract class ActivationCondition {
    abstract fun willActivate(
        boardManager: BoardStateManager,
        source: CardState,
        target: CardState,
        player: Player,
    ): Boolean

    abstract fun name(): String
}

class AllyKilled : ActivationCondition() {
    override fun willActivate(
        boardManager: BoardStateManager,
        source: CardState,
        target: CardState,
        player: Player,
    ): Boolean =
        boardManager
            .getBoardState()
            .traps[(!player).ordinal]
            .filter { value -> value?.id == TRAP_MOTHERS_LOVE_ID }
            .isNotEmpty()

    override fun name(): String = "ally_killed"
}
