/*
 * Created by BSGMatt on 2024.11.11
 */

package objects.passives

import objects.*
import objects.packets.*
import objects.packets.objects.*

abstract class Passive (
    //The passive manager
    open val passiveManager: PassiveManager,

    //The card this passive belongs to.
    open val card: Card,

    //The player that owns the card associated with this passive.
    open val player: Player
) {

    /*
    Update the state of the passive, return any actions for the update
     */
    abstract fun update(lastChange: Packet?, boardState: BoardState): CardActionList?
}

class DefaultPassive(passiveManager: PassiveManager, card: Card, player: Player) : Passive(passiveManager, card, player) {
    override fun update(lastChange: Packet?, boardState: BoardState): CardActionList? {
        print("A default passive update function was called for: $card");
        return CardActionList(card, arrayOf(CardAction(CardActionNames.TEST, arrayOf(card), 0)))
    }
}
