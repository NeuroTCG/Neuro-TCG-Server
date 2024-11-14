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

    //The next unique ID number to assign to a passive.
    val passives: HashMap<Card, Passive> = hashMapOf()

    fun addPassive(card: Card, player: Player) {
        val newPassive : Passive? = assignPassiveByCard(card, player)
        when (newPassive) {
            is Passive -> {
                passives.put(card, newPassive)
            }
            else -> {
                print("Tried to add passive of unknown card id.")
            }
        }
    }

    fun assignPassiveByCard(card: Card, player: Player): Passive? {
        when(card.id) {
            0 -> {
                return DefaultPassive(this, card, player)
            }
        }

        return null
    }
}
