package objects.packets.objects

import kotlinx.serialization.*
import objects.packets.*


@Serializable(with = CardTurnPhase.Serializer::class)
enum class CardTurnPhase {
    Done,
    Action,
    MoveOrAction;

    object Serializer: EnumOrdinalSerializer<CardTurnPhase>("CardTurnPhase", entries.toTypedArray())
}

@Suppress("PropertyName")
@Serializable
data class CardState(
    @Required var id: Int,
    @Required var health: Int,

    @Required var ability_was_used: Boolean = false,
    @Required var phase: CardTurnPhase = CardTurnPhase.MoveOrAction,
)
