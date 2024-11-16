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
    val passives: HashMap<CardData, Passive> = hashMapOf()

    fun addPassive(cardData: CardData, player: Player) {

        println("adding passive for $player 's $cardData Card.")

        val newPassive : Passive? = assignPassiveByCard(cardData, player)
        when (newPassive) {
            is Passive -> {
                println("adding new passive: $newPassive")
                passives[cardData] = newPassive
            }
            else -> {
                print("Tried to add passive of unknown card id.")
            }
        }

        println("passive Manager is now tracking ${passives.size} passives.")
    }

    fun assignPassiveByCard(cardData: CardData, player: Player): Passive? {
        println("Card's ID is ${cardData.id}");
        when(cardData.id) {
            //TODO: Create Unique passives for each card.
            0 -> {
                return DefaultPassive(this, cardData, player)
            }
            else -> {
                return DefaultPassive(this, cardData, player)
            }
        }

        return null
    }

    fun updatePassives(packet: Packet) : PassiveUpdatePacket {

        lastPacket = packet;
        val updateActions: MutableList<CardActionList> = mutableListOf();

        println("Updating ${passives.size} passives...")

        for (p : Passive in passives.values) {
            val updates : CardActionList? = p.update(lastPacket, boardManager.getBoardState())

            /**
             * NULL -> card has been destroyed, remove passive
             * Empty List -> no actions needed, don't add CardActionList to packet
             * Non-Empty List -> Add CardActionList to packet
             */
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
