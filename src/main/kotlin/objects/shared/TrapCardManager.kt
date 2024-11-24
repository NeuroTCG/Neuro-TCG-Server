package objects.shared

import objects.*
import objects.packets.*
import objects.packets.objects.*

class TrapCardManager(
    val manager: BoardStateManager,
) {
    val trapCards =
        CardStats.cardIDMapping.filter { entry -> entry.value.card_type == CardType.TRAP }

    private fun performConditionChecks(
        source: CardState,
        target: CardState,
        player: Player,
        trapCardStats: TrapCardStats,
    ): Boolean =
        when (trapCardStats.activation) {
            Activations.ALLY_KILLED -> AllyKilled().willActivate(manager, source, target, player)
        }

    /**
     * Call this when enemy is about to be destroyed
     * @return True if cancelled
     */
    suspend fun wouldDestroyEnemy(
        source: CardState,
        target: CardState,
        player: Player,
        damage: Int,
    ): Boolean {
        val passedCards =
            manager.getBoardState().traps[(!player).ordinal].filter { value ->
                performConditionChecks(
                    source,
                    target,
                    player,
                    CardStats.getCardByID(value!!.id)!!.trap_card_stats!!,
                )
            }

        for (card in passedCards) {
            val cardStat = CardStats.getCardByID(card!!.id)
            val trapCardStat = cardStat!!.trap_card_stats!!

            when (trapCardStat.effect) {
                Effects.A_MOTHERS_LOVE ->
                    AMothersLoveEffect(damage).doEffect(manager, source, target, player)
            }
        }

        return passedCards.isNotEmpty()
    }
}
