/*
 * Created by BSGMatt on 2024.11.11
 */

package objects.passives

import objects.*
import objects.packets.*
import objects.packets.objects.*
import kotlin.math.max

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

class AngelNeuroPassive(
    passiveManager: PassiveManager,
    cardData: CardData,
    player: Player,
) : Passive(passiveManager, cardData, player) {
    private val adjacentCards: MutableMap<CardData, CardData> = mutableMapOf()
    private var removedBuffsOnDestroy = false

    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList? {
        val removeBuffList: MutableList<CardActionTarget> = mutableListOf()
        val addBuffList: MutableList<CardActionTarget> = mutableListOf()
        val actions: MutableList<CardAction> = mutableListOf()

        if (cardWasDestroyed()) {
            if (!removedBuffsOnDestroy) {
                removedBuffsOnDestroy = true

                for (c: CardData in adjacentCards.values) {
                    removeBuffList.add(CardActionTarget(c.playerIdx, c.position))
                }

                val removeBuffArray = removeBuffList.toTypedArray()

                actions.add(CardAction(CardActionNames.SUB_ATTACK, removeBuffArray, 2))
                actions.add(CardAction(CardActionNames.SUB_HP, removeBuffArray, 2, arrayOf(CardActionArgs.minHp(1))))

                return CardActionList(cardData, actions.toTypedArray())
            }

            return null
        }

        // Get newly updated list of adjacentCards
        val newAdjacentCards: Map<CardData, CardData> = passiveManager.getAdjacentCards(cardData)

        // Nothing to be updated. Skip.
        if (newAdjacentCards.isEmpty() && adjacentCards.isEmpty()) {
            return CardActionList.emptyActionList(cardData)
        }

        for (c: CardData in newAdjacentCards.values) {
            if (!adjacentCards.containsKey(c)) {
                adjacentCards[c] = c
                c.state.health += 2
                c.state.attack_bonus += 2
                addBuffList.add(CardActionTarget(c.playerIdx, c.position))
            }
        }

        // Create a queue of cards to remove to avoid an exception getting thrown here.
        val removeQueue: MutableList<CardData> = mutableListOf()

        // Adding destroyed cards to the update packet causes an assert statement to fail on the client's end.
        // Making a separate list that removes them from the list without adding them to the update packet.
        val destroyedQueue: MutableList<CardData> = mutableListOf()

        for (c: CardData in adjacentCards.values) {
            if (cardWasDestroyed(c)) {
                destroyedQueue.add(c)
            } else if (!newAdjacentCards.containsKey(c)) {
                c.state.health = max(1, c.state.health - 2)
                c.state.attack_bonus -= 2
                removeQueue.add(c)
            }
        }
        for (c: CardData in destroyedQueue) {
            adjacentCards.remove(c)
        }
        for (c: CardData in removeQueue) {
            adjacentCards.remove(c)
            removeBuffList.add(CardActionTarget(c.playerIdx, c.position))
        }

        // Give new adjacentCards the +2 HP / +2 Attack Buff
        if (addBuffList.isNotEmpty()) {
            val addBuffArray = addBuffList.toTypedArray()
            actions.add(CardAction(CardActionNames.ADD_HP, addBuffArray, 2))
            actions.add(CardAction(CardActionNames.ADD_ATTACK, addBuffArray, 2))
        }

        // Remove buffs from cards no longer adjacent to angel
        if (removeBuffList.isNotEmpty()) {
            val removeBuffArray = removeBuffList.toTypedArray()
            actions.add(CardAction(CardActionNames.SUB_ATTACK, removeBuffArray, 2))
            actions.add(CardAction(CardActionNames.SUB_HP, removeBuffArray, 2, arrayOf(CardActionArgs.minHp(1))))
        }

        return CardActionList(cardData, actions.toTypedArray())
    }
}
