/*
 * Created by BSGMatt on 2024.11.11
 */

package objects.passives

import objects.*
import objects.packets.*
import objects.packets.objects.*

abstract class Passive(
    // The passive manager
    open val passiveManager: PassiveManager,
    // The card this passive belongs to.
    open val cardData: CardData,
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

    fun cardWasDestroyed(): Boolean = passiveManager.findCardByPosition(player, cardData.position) == null
}

class DefaultPassive(
    passiveManager: PassiveManager,
    cardData: CardData,
    player: Player,
) : Passive(passiveManager, cardData, player) {
    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList? {
        print("A default passive update function was called for: $cardData")

        // Check if the card has been destroyed
        if (cardWasDestroyed()) {
            return null
        }
        return CardActionList.testActionList(cardData, playerIdx())
    }
}

class NullPassive(
    passiveManager: PassiveManager,
    cardData: CardData,
    player: Player,
) : Passive(passiveManager, cardData, player) {
    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState,
    ): CardActionList? = null
}

class FilipinoBoyPassive(
    passiveManager: PassiveManager,
    cardData: CardData,
    player: Player,
) : Passive(passiveManager, cardData, player) {
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

                return CardActionList.drawCardActionList(cardData, playerIdx(), cardId)
            }
            return null
        } else {
            return CardActionList.emptyActionList(cardData)
        }
    }
}
