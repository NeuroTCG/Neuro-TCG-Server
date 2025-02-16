/*
 * Created by BSGMatt on 2024.11.11
 */

package objects.passives

import objects.*
import objects.packets.*
import objects.packets.objects.*
import kotlin.math.max

abstract class PassiveEffect(
    // The passive manager
    open val passiveManager: PassiveManager,
    // The card this passive belongs to.
    open val card: Card,
    // The player that owns the card associated with this passive.
    open val player: Player,
) {
    /*
    Update the state of the passive, return any actions for the update
     */
    abstract suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList?

    fun playerIdx(): Int = passiveManager.playerToIdx(player)

    fun cardWasDestroyed(it: Card = card): Boolean =
        passiveManager.findCardByPosition(passiveManager.idxToPlayer(it.playerIdx), it.position) == null
}

class NullPassive(
    passiveManager: PassiveManager,
    card: Card,
    player: Player,
) : PassiveEffect(passiveManager, card, player) {
    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList? = null
}

class DrawOnDestruction(
    passiveManager: PassiveManager,
    card: Card,
    player: Player,
) : PassiveEffect(passiveManager, card, player) {
    private var drawRequestWasSent = false

    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList? {
        // Card has been destroyed
        if (cardWasDestroyed()) {
            if (!drawRequestWasSent) {
                drawRequestWasSent = true

                // Draw card
                val cardId = passiveManager.drawCard(player)

                return CardActionList.drawCardActionList(card, playerIdx(), cardId)
            }
            return null
        } else {
            return CardActionList.emptyActionList(card)
        }
    }
}

class BuffAdjacent(
    passiveManager: PassiveManager,
    card: Card,
    player: Player,
) : PassiveEffect(passiveManager, card, player) {
    private val adjacentCards: MutableMap<Card, Card> = mutableMapOf()
    private var removedBuffsOnDestroy = false

    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList? {
        val removeBuffList: MutableList<CardActionTarget> = mutableListOf()
        val addBuffList: MutableList<CardActionTarget> = mutableListOf()
        val actions: MutableList<CardAction> = mutableListOf()

        // Assign values for atk and hp increase
        var atkIncrease = 0
        var hpIncrease = 0
        val stats: CardStats? = CardStats.getCardByID(card.state.id)
        stats?.let {
            cardStats ->
            atkIncrease = stats.passive.values[0]
            hpIncrease = stats.passive.values[1]
        } ?: run {
            println("Warning: no card was found with ID ${card.state.id}")
        }

        if (cardWasDestroyed()) {
            if (!removedBuffsOnDestroy) {
                removedBuffsOnDestroy = true

                for (c: Card in adjacentCards.values) {
                    removeBuffList.add(CardActionTarget(c.playerIdx, c.position))
                }

                val removeBuffArray = removeBuffList.toTypedArray()

                actions.add(CardAction(CardActionNames.SUB_ATTACK, removeBuffArray, atkIncrease))
                actions.add(CardAction(CardActionNames.SUB_HP, removeBuffArray, hpIncrease, arrayOf(CardActionArgs.minHp(1))))

                return CardActionList(card, actions.toTypedArray())
            }

            return null
        }

        // Get newly updated list of adjacentCards
        val newAdjacentCards: Map<Card, Card> = passiveManager.getAdjacentCards(card)

        // Nothing to be updated. Skip.
        if (newAdjacentCards.isEmpty() && adjacentCards.isEmpty()) {
            return CardActionList.emptyActionList(card)
        }

        for (c: Card in newAdjacentCards.values) {
            if (!adjacentCards.containsKey(c)) {
                adjacentCards[c] = c
                c.state.health += atkIncrease
                c.state.attack_bonus += hpIncrease
                addBuffList.add(CardActionTarget(c.playerIdx, c.position))
            }
        }

        // Create a queue of cards to remove to avoid an exception getting thrown here.
        val removeQueue: MutableList<Card> = mutableListOf()

        // Adding destroyed cards to the update packet causes an assert statement to fail on the client's end.
        // Making a separate list that removes them from the list without adding them to the update packet.
        val destroyedQueue: MutableList<Card> = mutableListOf()

        for (c: Card in adjacentCards.values) {
            if (cardWasDestroyed(c)) {
                destroyedQueue.add(c)
            } else if (!newAdjacentCards.containsKey(c)) {
                c.state.health = max(1, c.state.health - hpIncrease)
                c.state.attack_bonus -= atkIncrease
                removeQueue.add(c)
            }
        }
        for (c: Card in destroyedQueue) {
            adjacentCards.remove(c)
        }
        for (c: Card in removeQueue) {
            adjacentCards.remove(c)
            removeBuffList.add(CardActionTarget(c.playerIdx, c.position))
        }

        // Give new adjacentCards the +x HP / +y Attack Buff
        if (addBuffList.isNotEmpty()) {
            val addBuffArray = addBuffList.toTypedArray()
            actions.add(CardAction(CardActionNames.ADD_HP, addBuffArray, hpIncrease))
            actions.add(CardAction(CardActionNames.ADD_ATTACK, addBuffArray, atkIncrease))
        }

        // Remove buffs from cards no longer adjacent to angel
        if (removeBuffList.isNotEmpty()) {
            val removeBuffArray = removeBuffList.toTypedArray()
            actions.add(CardAction(CardActionNames.SUB_ATTACK, removeBuffArray, hpIncrease))
            actions.add(CardAction(CardActionNames.SUB_HP, removeBuffArray, atkIncrease, arrayOf(CardActionArgs.minHp(1))))
        }

        return CardActionList(card, actions.toTypedArray())
    }
}
