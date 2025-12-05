package objects.passives

import objects.CardAction
import objects.CardActionList
import objects.CardActionNames
import objects.Player
import objects.packets.EndTurnPacket
import objects.packets.Packet
import objects.packets.StartTurnPacket
import objects.packets.objects.BoardState
import objects.packets.objects.Card
import objects.packets.objects.CardPosition
import okhttp3.internal.toImmutableList
import kotlin.random.Random

/**
 * Card will randomly bounce to another empty space at the end of the player's turn.
 * (including its current one.) If it bounces into a different spot, and if that spot is
 * a corner, then it shall receive a buff.
 */
class RandomBounce(passiveManager: PassiveManager, card: Card, player: Player) :
    PassiveEffect(passiveManager, card, player) {

    var startingPosition: CardPosition? = null

    override suspend fun update(
        lastChange: Packet?,
        boardState: BoardState
    ): CardActionList? {

        val cardActions: MutableList<CardAction> = mutableListOf()

        //New turn; Set starting position.
        if (lastChange is StartTurnPacket && passiveManager.isTurnOfPlayer(player)) {
            startingPosition = card.position
            return CardActionList.emptyActionList(card);
        }

        if (lastChange is EndTurnPacket) {
            val actions = jumpToRandomSpot().toTypedArray()

            if (actions.isNotEmpty()) {
                return CardActionList(card, actions)
            }
        }

        return CardActionList.emptyActionList(card);
    }

    private fun jumpToRandomSpot() : List<CardAction> {
        val possibleSpots: List<CardPosition> = mutableListOf(card.position).apply {
            addAll(passiveManager.findEmptySpotsInField(player))
        }.toImmutableList()

        //Select one of the possible spots.
        val newSpot: Int = Random.nextInt(0, possibleSpots.size);

        if (possibleSpots[newSpot] != card.position) {

            val moveToNewSpotAction: CardAction =
                passiveManager.handleMoveCardAction(card, player, possibleSpots[newSpot])
            return listOf(moveToNewSpotAction)
        }

        return listOf()
    }


}
