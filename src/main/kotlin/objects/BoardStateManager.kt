package objects

import objects.shared.*
import kotlin.math.*

class BoardStateManager(val db: GameDatabase) {
    private var boardState = BoardState()
    val player1ID: Int = 1
    val player2ID: Int = 2
    val gameID = db.createGame(player1ID, player2ID)

    fun getBoardState(): BoardState {
        return this.boardState
    }

    fun summon(playerIndex: Int, row: Int, column: Int, cardID: Int) {
        this.boardState.rows[playerIndex][row][column] = PlayedCard(cardID)
    }

    fun attack(attackingPlayer: Int, attackingRow: Int, attackingColumn: Int, targetRow: Int, targetColumn: Int) {
        val attacker = boardState.rows[attackingPlayer][attackingRow][attackingColumn]
        val defender = boardState.rows[1 - attackingPlayer][targetRow][targetColumn]

        if (attacker == null)
            throw InvalidMoveException("Trying to attack with nothing")
        if (defender == null)
            throw InvalidMoveException("Trying to attack nothing")

        defender.HP -= CardStats.getCardByID(attacker.id).baseATK
        attacker.HP -= max(CardStats.getCardByID(defender.id).baseATK - 1, 0)
    }
}
