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
                print("Tried to add passive of unknown card id.")
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
            0 -> {
                return DefaultPassive(this, cardData, player)
            }
            CardIDNames.FILIPINO_BOY.ordinal -> {
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
            } else if (updates!!.actions.isNotEmpty()) {
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

    suspend fun drawCard(player: Player): Int {
        val cardID = boardManager.cardDecks[playerToIdx(player)].drawCard()
        boardManager.placeInHand(player, cardID)

        return cardID
    }
}
