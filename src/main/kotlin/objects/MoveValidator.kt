package objects

class MoveValidator {
    // TODO: Implement actual validation logic.
    suspend fun checkPlayerHand(
        playerIndex: Int,
        boardState: MutableList<MutableList<MutableList<Int>>>
    ): Boolean {
        val currentPlayer = boardState[playerIndex]
        val opponentPlayer = boardState[1 - playerIndex]
        val valid: Boolean = true
        return valid
    }
    suspend fun checkIfTurn(
        playerIndex: Int,
        boardState: MutableList<MutableList<MutableList<Int>>>
    ): Boolean {
        val currentPlayer = boardState[playerIndex]
        val opponentPlayer = boardState[1 - playerIndex]
        val valid: Boolean = true
        return valid
    }
    suspend fun validate(
        playerIndex: Int,
        row: Int,
        column: Int,
        boardState: MutableList<MutableList<MutableList<Int>>>
    ) : Boolean {
        val currentPlayer = boardState[playerIndex]
        val opponentPlayer = boardState[1 - playerIndex]
        val currentPlayerRow = currentPlayer[row]
        val valid: Boolean = true
        return valid}
}
