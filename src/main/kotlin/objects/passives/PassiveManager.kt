/*
 * Created by BSGMatt on 2024.11.11
 */

package objects.passives

import objects.*
import objects.packets.*
import objects.packets.objects.*

class PassiveManager (
    val boardManager: BoardStateManager
) {
    //A Packet used to describe the last action that occurred.
    // Used to provide context when updating passives.
    var lastPacket: Packet? = null;
    val passives: HashMap<Card, Passive> = hashMapOf()

    fun addPassive(card: Card, player: Player) {

        println("adding passive for $player 's $card Card.")

        val newPassive : Passive? = assignPassiveByCard(card, player)
        when (newPassive) {
            is Passive -> {
                println("adding new passive: $newPassive")
                passives[card] = newPassive
            }
            else -> {
                print("Tried to add passive of unknown card id.")
            }
        }

        println("passive Manager is now tracking ${passives.size} passives.")
    }

    fun assignPassiveByCard(card: Card, player: Player): Passive? {
        println("Card's ID is ${card.id}");
        when(card.id) {
            //TODO: Create Unique passives for each card.
            0 -> {
                return DefaultPassive(this, card, player)
            }
            else -> {
                return DefaultPassive(this, card, player)
            }
        }

        return null
    }

    fun updatePassives(packet: Packet?) : PassiveUpdatePacket? {

        if (packet == null) {
            return null;
        }

        lastPacket = packet;
        val updateActions: MutableList<CardActionList> = mutableListOf();

        println("Updating ${passives.size} passives...")

        for (p : Passive in passives.values) {
            val updates : CardActionList? = p.update(lastPacket, boardManager.getBoardState())
            if (updates != null) {
                print(updates);
                updateActions.add(updates);
            }
            else {
                print("$p returned a empty array of update actions.");
            }
        }

        return PassiveUpdatePacket(updateActions.toTypedArray());
    }
}
