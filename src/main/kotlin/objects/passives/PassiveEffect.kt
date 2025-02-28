/*
 * Created by BSGMatt on 2024.11.11
 */

package objects.passives

import objects.*
import objects.packets.*
import objects.packets.objects.*
import kotlin.math.*

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
        stats?.let { cardStats ->
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

class CardDiscount(
    passiveManager: PassiveManager,
    card: Card,
    player: Player,
) : PassiveEffect(passiveManager, card, player) {
    // Keep track of discount values in a dictionary so that we don't interfere with other abilities that
    // may modify the cost of a card's ability.
    private val currentDiscountValues: MutableMap<Card, Int> = mutableMapOf()
    private var resetDiscountOnDestroy = false

    private var discountAmount = 0
    private var minAbilityCost = 0
    private var cardType = 0

    init {
        val stats: CardStats? = CardStats.getCardByID(card.state.id)
        stats?.let { cardStats ->
            discountAmount = stats.passive.values[0]
            minAbilityCost = stats.passive.values[1]
            cardType = stats.passive.values[2]
        } ?: run {
            println("Warning: no card was found with ID ${card.state.id}")
        }
    }

    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList? {
        val actions: MutableList<CardAction> = mutableListOf()

        val removeBuffList: MutableList<Card> = mutableListOf()

        if (cardWasDestroyed()) {
            if (resetDiscountOnDestroy) {
                return null
            } else {
                resetDiscountOnDestroy = true

                for (c: Card in currentDiscountValues.keys) {
                    val discount = currentDiscountValues[c]

                    if (discount == null) {
                        assert(false, { "Could not find a card with value: $c" })
                    } else {
                        c.state.ability_cost_modifier += discount

                        actions.add(
                            CardAction(
                                CardActionNames.ADD_ABILITY_COST_MODIFIER,
                                arrayOf(CardActionTarget(playerIdx(), c.position)),
                                discount,
                            ),
                        )
                    }
                }

                return CardActionList(card, actions.toTypedArray())
            }
        }

        val newMagicCardMap = passiveManager.getCardsInFieldOfType(player, CardType.entries[cardType])

        for (c: Card in newMagicCardMap.values) {
            if (!currentDiscountValues.containsKey(c)) {
                var discount = 0
                if (c.state.currentAbilityCost() - discountAmount < minAbilityCost) {
                    discount = c.state.currentAbilityCost() - minAbilityCost
                } else {
                    discount = discountAmount
                }

                currentDiscountValues[c] = discount
                c.state.ability_cost_modifier -= discount
                actions.add(
                    CardAction(CardActionNames.SUB_ABILITY_COST_MODIFIER, arrayOf(CardActionTarget(playerIdx(), c.position)), discount),
                )
            }
        }

        for (c: Card in currentDiscountValues.keys) {
            // Remove if card is no longer in player's field
            if (!newMagicCardMap.containsKey(c)) {
                removeBuffList.add(c)

                val discount = currentDiscountValues[c]!!

                c.state.ability_cost_modifier += discount

                actions.add(
                    CardAction(CardActionNames.ADD_ABILITY_COST_MODIFIER, arrayOf(CardActionTarget(playerIdx(), c.position)), discount),
                )
            }
        }

        for (c: Card in removeBuffList) {
            currentDiscountValues.remove(c)
        }

        return CardActionList(card, actions.toTypedArray())
    }
}
