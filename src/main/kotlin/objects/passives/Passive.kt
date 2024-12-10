/*
 * Created by BSGMatt on 2024.11.11
 */

package objects.passives

import objects.*
import objects.packets.*
import objects.packets.objects.*

abstract class Passive(
    // The passive manager
    open val passiveManager: PassiveManager,
    // The card this passive belongs to.
    open val cardData: CardData,
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

    fun cardWasDestroyed(it: CardData = cardData): Boolean =
        passiveManager.findCardByPosition(passiveManager.idxToPlayer(it.playerIdx), it.position) == null
}

class DefaultPassive(
    passiveManager: PassiveManager,
    cardData: CardData,
    player: Player,
) : Passive(passiveManager, cardData, player) {
    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList? {
        print("A default passive update function was called for: $cardData")

        // Check if the card has been destroyed
        if (cardWasDestroyed()) {
            return null
        }
        return CardActionList.testActionList(cardData, playerIdx())
    }
}

class NullPassive(
    passiveManager: PassiveManager,
    cardData: CardData,
    player: Player,
) : Passive(passiveManager, cardData, player) {
    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList? = null
}

/**
 *  Passive for 'Filipino Boy'
 *      Card holder draws card when destroyed
 */
class FilipinoBoyPassive(
    passiveManager: PassiveManager,
    cardData: CardData,
    player: Player,
) : Passive(passiveManager, cardData, player) {
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

                return CardActionList.drawCardActionList(cardData, playerIdx(), cardId)
            }
            return null
        } else {
            return CardActionList.emptyActionList(cardData)
        }
    }
}

/**
 *  Passive for 'Angel Neuro'
 *      Adjacent Cards gain +1 HP / +1 Attack
 */
class AngelNeuroPassive(
    passiveManager: PassiveManager,
    cardData: CardData,
    player: Player,
) : Passive(passiveManager, cardData, player) {
    private val adjacentCards: MutableMap<CardData, CardData> = mutableMapOf()

    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList? {
        val removeBuffList: MutableList<CardActionTarget> = mutableListOf()
        val addBuffList: MutableList<CardActionTarget> = mutableListOf()
        val actions: MutableList<CardAction> = mutableListOf()

        if (cardWasDestroyed()) {
            return null
        }

        // Get newly updated list of adjacentCards
        val newAdjacentCards: Map<CardData, CardData> = passiveManager.getAdjcentCards(cardData)

        // Nothing to be updated. Skip.
        if (newAdjacentCards.isEmpty() && adjacentCards.isEmpty()) {
            return CardActionList.emptyActionList(cardData)
        }

        for (c: CardData in newAdjacentCards.values) {
            if (!adjacentCards.containsKey(c)) {
                adjacentCards[c] = c
                c.state.health += 2
                addBuffList.add(CardActionTarget(c.playerIdx, c.position))
            }
        }

        // Create a queue of cards to remove to avoid an exception getting thrown here.
        val removeQueue: MutableList<CardData> = mutableListOf()
        for (c: CardData in adjacentCards.values) {
            if (cardWasDestroyed(c)) {
                removeQueue.add(c)
            } else if (!newAdjacentCards.containsKey(c)) {
                c.state.health = if (c.state.health - 2 < 1) 1 else c.state.health - 2
                removeQueue.add(c)
            }
        }
        for (c: CardData in removeQueue) {
            adjacentCards.remove(c)
            removeBuffList.add(CardActionTarget(c.playerIdx, c.position))
        }

        // Give new adjacentCards the +2 HP / +2 Attack Buff
        if (addBuffList.isNotEmpty()) {
            actions.add(CardAction(CardActionNames.ADD_HP, addBuffList.toTypedArray(), 2))
            actions.add(CardAction(CardActionNames.ADD_ATTACK, addBuffList.toTypedArray(), 2))
        }

        // Remove buffs from cards no longer adjacent to neuro
        if (removeBuffList.isNotEmpty()) {
            actions.add(CardAction(CardActionNames.SUB_ATTACK, removeBuffList.toTypedArray(), 2))
            actions.add(CardAction(CardActionNames.SUB_HP, removeBuffList.toTypedArray(), 2, arrayOf(CardActionArgs.minHp(1))))
        }

        return CardActionList(cardData, actions.toTypedArray())
    }
}
