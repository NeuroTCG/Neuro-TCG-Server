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
    open val cardData: CardData,

    //The player that owns the card associated with this passive.
    open val player: Player
) {

    /*
    Update the state of the passive, return any actions for the update
     */
    abstract fun update(lastChange: Packet?, boardState: BoardState): CardActionList?
}

class DefaultPassive(passiveManager: PassiveManager, cardData: CardData, player: Player) : Passive(passiveManager, cardData, player) {
    override fun update(lastChange: Packet?, boardState: BoardState): CardActionList? {
        print("A default passive update function was called for: $cardData");
        return CardActionList(cardData, arrayOf(CardAction(CardActionNames.TEST, arrayOf(cardData.position), 0)))
    }
}
