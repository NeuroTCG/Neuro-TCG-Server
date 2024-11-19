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
            else -> {
                return DefaultPassive(this, cardData, player)
            }
        }

        return null
    }

    fun updatePassives(packet: Packet): PassiveUpdatePacket {
        lastPacket = packet
        val updateActions: MutableList<CardActionList> = mutableListOf()

        println("Updating ${passives.size} passives...")

        for (p: Passive in passives.values) {
            val updates: CardActionList? = p.update(lastPacket, boardManager.getBoardState())

            if (updates == null) {
                // NULL -> card has been destroyed, remove passive
                passives.remove(p.cardData)
            }
            if (updates!!.actions.isNotEmpty()) {
                // Non-Empty List -> Add CardActionList to packet
                updateActions.add(updates)
            }

            // Empty List -> no actions needed, don't add CardActionList to packet
        }

        return PassiveUpdatePacket(updateActions.toTypedArray())
    }
}
