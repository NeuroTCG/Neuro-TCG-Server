/*
 * Created by BSGMatt on 2024.11.11
 */

package objects.passives

import objects.*
import objects.packets.*
import objects.packets.objects.*

class PassiveManager(
    private val boardManager: BoardStateManager,
) {
    // A Packet used to describe the last action that occurred.
    // Used to provide context when updating passives.
    var lastPacket: Packet? = null
    val passives: HashMap<CardData, Passive> = hashMapOf()

    fun playerToIdx(player: Player): Int {
        if (player == Player.Player1) {
            return 0
        } else {
            return 1
        }
    }

    fun idxToPlayer(idx: Int): Player {
        if (idx == 0) {
            return Player.Player1
        } else {
            return Player.Player2
        }
    }

    fun addPassive(
        cardData: CardData,
        player: Player,
    ) {
        val newPassive: Passive? = assignPassiveByCard(cardData, player)
        when (newPassive) {
            is Passive -> {
                println("adding new passive: $newPassive")
                passives[cardData] = newPassive
            }
            else -> {
                assert(false, { "Tried to add passive of unknown card id." })
            }
        }
    }

    fun assignPassiveByCard(
        cardData: CardData,
        player: Player,
    ): Passive? {
        println("Card's ID is ${cardData.state.id}")
        when (cardData.state.id) {
            // TODO: Create Unique passives for each card.
            CardIDNumbers.PIRATE_EVIL -> {
                return DefaultPassive(this, cardData, player)
            }
            CardIDNumbers.ANGEL_NEURO -> {
                return AngelNeuroPassive(this, cardData, player)
            }
            CardIDNumbers.FILIPINO_BOY -> {
                return FilipinoBoyPassive(this, cardData, player)
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
        val removalQueue: MutableList<CardData> = mutableListOf()

        println("Updating ${passives.size} passives...")

        for (p: Passive in passives.values) {
            val updates: CardActionList? = p.update(lastPacket, boardManager.getBoardState())

            if (updates == null) {
                // NULL -> passive is ready to be removed from list
                removalQueue.add(p.cardData)
            } else if (updates.actions.isNotEmpty()) {
                // Non-Empty List -> Add CardActionList to packet
                updateActions.add(updates)
            }

            // Empty List -> no actions needed, don't add CardActionList to packet
        }

        for (c: CardData in removalQueue) {
            assert(passives.remove(c) != null, { "Passive associated with CardData object " + c + " does not exist." })
        }

        return PassiveUpdatePacket(updateActions.toTypedArray())
    }

    fun findCardByPosition(
        player: Player,
        position: CardPosition,
    ): CardData? = boardManager.getCard(player, position)

    fun getAdjcentCards(thisCard: CardData): Map<CardData, CardData> {
        val workingList: MutableMap<CardData, CardData> = mutableMapOf()

        val leftPosition: CardPosition? = positionLeftOf(thisCard.position)
        if (leftPosition != null) {
            val leftCard: CardData? = boardManager.getCard(idxToPlayer(thisCard.playerIdx), leftPosition)
            if (leftCard != null) {
                workingList.put(leftCard, leftCard)
            }
        }

        val rightPosition: CardPosition? = positionRightOf(thisCard.position)
        if (rightPosition != null) {
            val rightCard: CardData? = boardManager.getCard(idxToPlayer(thisCard.playerIdx), rightPosition)
            if (rightCard != null) {
                workingList.put(rightCard, rightCard)
            }
        }

        return workingList.toMap()
    }

    private fun positionLeftOf(position: CardPosition): CardPosition? {
        val leftColumn = if (position.column - 1 < 0) 0 else position.column - 1

        // Card is already on left end of row
        if (position.column == leftColumn) return null
        return CardPosition(position.row, leftColumn)
    }

    private fun positionRightOf(position: CardPosition): CardPosition? {
        val end = boardManager.getBoardState().cards[0][position.row].size - 1
        val rightColumn = if (position.column + 1 > end) end else position.column + 1

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
