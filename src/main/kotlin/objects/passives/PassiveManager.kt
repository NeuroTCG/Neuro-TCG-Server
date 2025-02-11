/*
 * Created by BSGMatt on 2024.11.11
 */

package objects.passives

import objects.*
import objects.packets.*
import objects.packets.objects.*
import kotlin.math.max
import kotlin.math.min

class PassiveManager(
    private val boardManager: BoardStateManager,
) {
    // A Packet used to describe the last action that occurred.
    // Used to provide context when updating passives.
    var lastPacket: Packet? = null
    val passives: HashMap<Card, PassiveEffect> = hashMapOf()

    fun playerToIdx(player: Player): Int {
        if (player == Player.Player1) {
            return 0
        } else {
            return 1
        }
    }

    fun idxToPlayer(idx: Int): Player {
        assert(idx == 0 || idx == 1, { "player index out of range!" })
        if (idx == 0) {
            return Player.Player1
        } else {
            return Player.Player2
        }
    }

    fun addPassive(
        card: Card,
        player: Player,
    ) {
        val newPassive: PassiveEffect? = assignPassiveByCard(card, player)
        when (newPassive) {
            is NullPassive -> {
                // Ignore
            }
            is PassiveEffect -> {
                println("adding new passive: $newPassive")
                passives[card] = newPassive
            }
            else -> {
                assert(false) {
                    "Tried to add passive of unknown card id. +" +
                        "If a card has no passive associated with it, use NullPassive instead."
                }
            }
        }
    }

    fun assignPassiveByCard(
        card: Card,
        player: Player,
    ): PassiveEffect? {
        println("Card's ID is ${card.state.id}")
        when (CardStats.getCardByID(card.state.id)?.passive?.effect) {
            // TODO: Create Unique passives for each card.
            PassiveEffectType.NONE -> {
                return NullPassive(this, card, player)
            }
            PassiveEffectType.BUFF_ADJACENT -> {
                return BuffAdjacent(this, card, player)
            }
            PassiveEffectType.DRAW_ON_DESTRUCTION -> {
                return DrawOnDestruction(this, card, player)
            }
            else -> {
                return null
            }
        }
    }

    suspend fun updatePassives(packet: Packet): PassiveUpdatePacket {
        lastPacket = packet
        val updateActions: MutableList<CardActionList> = mutableListOf()

        // Track list of cards needed to be removed.
        // That way, we're not removing passive as we iterate!
        val removalQueue: MutableList<Card> = mutableListOf()

        println("Updating ${passives.size} passives...")

        for (p: PassiveEffect in passives.values) {
            val updates: CardActionList? = p.update(lastPacket, boardManager.getBoardState())

            if (updates == null) {
                // NULL -> passive is ready to be removed from list
                removalQueue.add(p.card)
            } else if (updates.actions.isNotEmpty()) {
                // Non-Empty List -> Add CardActionList to packet
                updateActions.add(updates)
            }

            // Empty List -> no actions needed, don't add CardActionList to packet
        }

        for (c: Card in removalQueue) {
            assert(passives.remove(c) != null, { "Passive associated with Card object " + c + " does not exist." })
        }

        return PassiveUpdatePacket(updateActions.toTypedArray())
    }

    fun findCardByPosition(
        player: Player,
        position: CardPosition,
    ): Card? = boardManager.getCard(player, position)

    fun getAdjacentCards(thisCard: Card): Map<Card, Card> {
        val workingList: MutableMap<Card, Card> = mutableMapOf()

        val leftPosition: CardPosition? = positionLeftOf(thisCard.position)
        if (leftPosition != null) {
            val leftCard: Card? = boardManager.getCard(idxToPlayer(thisCard.playerIdx), leftPosition)
            if (leftCard != null) {
                workingList.put(leftCard, leftCard)
            }
        }

        val rightPosition: CardPosition? = positionRightOf(thisCard.position)
        if (rightPosition != null) {
            val rightCard: Card? = boardManager.getCard(idxToPlayer(thisCard.playerIdx), rightPosition)
            if (rightCard != null) {
                workingList.put(rightCard, rightCard)
            }
        }

        return workingList.toMap()
    }

    private fun positionLeftOf(position: CardPosition): CardPosition? {
        val leftColumn = max(0, position.column - 1)

        // Card is already on left end of row
        if (position.column == leftColumn) return null
        return CardPosition(position.row, leftColumn)
    }

    private fun positionRightOf(position: CardPosition): CardPosition? {
        val end = boardManager.getBoardState().cards[0][position.row].size - 1
        val rightColumn = min(end, position.column + 1)

        // Check if card is already on the right side
        if (position.column == rightColumn) return null
        return CardPosition(position.row, rightColumn)
    }

    suspend fun drawCard(player: Player): Int {
        val cardID = boardManager.cardDecks[playerToIdx(player)].drawCard()
        boardManager.placeInHand(player, cardID)

        return cardID
    }
}
