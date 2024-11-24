package objects.shared

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import objects.*
import objects.packets.*
import objects.packets.objects.*
import objects.packets.objects.CardStats.Companion.TRAP_MOTHERS_LOVE_ID

enum class Effects {
    @SerialName("a_mothers_love")
    A_MOTHERS_LOVE,
}

abstract class TrapCardEffect {
    abstract suspend fun doEffect(
        boardManager: BoardStateManager,
        source: CardState,
        target: CardState,
        player: Player,
    )

    abstract fun effectName(): String
}

class AMothersLoveEffect(
    val atk: Int,
) : TrapCardEffect() {
    override suspend fun doEffect(
        boardManager: BoardStateManager,
        source: CardState,
        target: CardState,
        player: Player,
    ) {
        // TODO: grant shield sptate, maintain one health, counterattack with atk
        boardManager.getConnection(player).sendPacket(
            TrapActivatedPacket(
                TRAP_MOTHERS_LOVE_ID,
                false,
            ),
        )

        boardManager.getConnection(!player).sendPacket(
            TrapActivatedPacket(
                TRAP_MOTHERS_LOVE_ID,
                true,
            ),
        )
    }

    override fun effectName(): String = "mothers_love_effect"
}
