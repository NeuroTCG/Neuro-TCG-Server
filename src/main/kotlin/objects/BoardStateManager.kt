package objects

import objects.packets.*
import objects.packets.objects.*
import kotlin.math.*

class BoardStateManager(
    val db: GameDatabase,
    val player1Connection: GameConnection,
    val player2Connection: GameConnection?
) {
    private var boardState = BoardState()
    val player1ID: Int = 1
    val player2ID: Int = 2
    val gameID = db.createGame(player1ID, player2ID)

    var currentPlayer: Int = player1ID
    var otherPlayer: Int = player2ID

    var currentPlayerConnection = player1Connection
    var currentOpponentConnection = player2Connection

    fun getBoardState(): BoardState {
        return this.boardState
    }

    private fun isTurnOfPlayer(isFirstPlayer: Boolean): Boolean {
        return (player1ID == currentPlayer) and isFirstPlayer
    }

    private fun playerBoolToIndex(isFirstPlayer: Boolean): Int {
        return if (isFirstPlayer) 0
        else 1
    }

    private fun getCard(isFirstPlayer: Boolean, position: CardPosition): CardState? {
        return this.boardState.cards[playerBoolToIndex(isFirstPlayer)][position.row][position.column]
    }

    private fun setCard(isFirstPlayer: Boolean, position: CardPosition, card: CardState?) {
        this.boardState.cards[playerBoolToIndex(isFirstPlayer)][position.row][position.column] = card
    }

    suspend fun handleSummonPacket(packet: SummonRequestPacket, isFirstPlayer: Boolean) {
        if (!isTurnOfPlayer(isFirstPlayer)) {
            currentPlayerConnection.sendPacket(packet.getResponsePacket(is_you = true, valid = false, new_card = null))
            return
        }
        if (getCard(isFirstPlayer, packet.position) != null) {
            currentPlayerConnection.sendPacket(packet.getResponsePacket(true, valid = false, new_card = null))
            return
        }

        val newCardState = CardState(packet.card_id, CardStats.getCardByID(packet.card_id).max_hp)
        setCard(
            isFirstPlayer,
            packet.position,
            newCardState
        )

        currentPlayerConnection.sendPacket(packet.getResponsePacket(true, valid = true, new_card = newCardState))
        currentOpponentConnection?.sendPacket(
            packet.getResponsePacket(
                is_you = false,
                valid = true,
                new_card = newCardState
            )
        )
    }


    suspend fun handleAttackPacket(packet: AttackRequestPacket, isFirstPlayer: Boolean) {
        if (!isTurnOfPlayer(isFirstPlayer)) {
            currentPlayerConnection.sendPacket(packet.getResponsePacket(
                is_you = true,
                valid = false,
                target_card = null,
                attacker_card = null
            ))
            return
        }

        val attacker = getCard(isFirstPlayer, packet.attacker_position)
        val target = getCard(!isFirstPlayer, packet.target_position)

        if (attacker == null || target == null) {
            currentPlayerConnection.sendPacket(packet.getResponsePacket(
                is_you = true,
                valid = false,
                target_card = null,
                attacker_card = null
            ))
            return
        }

        target.health -= CardStats.getCardByID(attacker.id).base_atk
        attacker.health -= max(CardStats.getCardByID(target.id).base_atk - 1, 0)

        setCard(isFirstPlayer, packet.attacker_position, attacker)
        setCard(!isFirstPlayer, packet.target_position, target)

        currentPlayerConnection.sendPacket(packet.getResponsePacket(
            is_you = true,
            valid = true,
            target_card = target,
            attacker_card = attacker
        ))
        currentOpponentConnection?.sendPacket(packet.getResponsePacket(false,
            valid = true,
            target_card = target,
            attacker_card = attacker
        ))
    }
}
