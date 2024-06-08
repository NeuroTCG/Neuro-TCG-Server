package objects

import objects.packets.*
import objects.shared.*
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

    private fun getCard(isFirstPlayer: Boolean, position: CardPosition): FullCardState? {
        return this.boardState.rows[playerBoolToIndex(isFirstPlayer)][position.row][position.column]
    }

    private fun setCard(isFirstPlayer: Boolean, position: CardPosition, card: FullCardState?) {
        this.boardState.rows[playerBoolToIndex(isFirstPlayer)][position.row][position.column] = card
    }

    suspend fun handleSummonPacket(packet: SummonPacket, isFirstPlayer: Boolean) {
        if (!isTurnOfPlayer(isFirstPlayer)) {
            currentPlayerConnection.sendPacket(packet.getResponsePacket(false, null))
            return
        }
        if (getCard(isFirstPlayer, packet.position) != null) {
            currentPlayerConnection.sendPacket(packet.getResponsePacket(false, null))
            return
        }

        val newCardState = FullCardState(packet.card_id, CardStats.getCardByID(packet.card_id).maxHP)
        setCard(
            isFirstPlayer,
            packet.position,
            newCardState
        )

        currentPlayerConnection.sendPacket(packet.getResponsePacket(true, newCardState))
        currentOpponentConnection?.sendPacket(packet.getOpponentPacket(newCardState))
    }


    suspend fun handleAttackPacket(packet: AttackPacket, isFirstPlayer: Boolean) {
        if (!isTurnOfPlayer(isFirstPlayer)) {
            currentPlayerConnection.sendPacket(packet.getResponsePacket(false, null, null))
            return
        }

        val attacker = getCard(isFirstPlayer, packet.attacker_position)
        val target = getCard(!isFirstPlayer, packet.target_position)

        if (attacker == null || target == null) {
            currentPlayerConnection.sendPacket(packet.getResponsePacket(false, null, null))
            return
        }

        target.health -= CardStats.getCardByID(attacker.id).baseATK
        attacker.health -= max(CardStats.getCardByID(target.id).baseATK - 1, 0)

        setCard(isFirstPlayer, packet.attacker_position, attacker)
        setCard(!isFirstPlayer, packet.target_position, target)

        currentPlayerConnection.sendPacket(packet.getResponsePacket(true, target, attacker))
        currentOpponentConnection?.sendPacket(packet.getOpponentPacket(target, attacker))
    }
}
