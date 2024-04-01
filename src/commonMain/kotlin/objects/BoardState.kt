package objects

class BoardState {
    // Initial Board State:
    // [
    // [[0,0,0,0],[0,0,0],[0,0]]
    // [[0,0,0,0],[0,0,0],[0,0]]
    // ]
    private val boardState = mutableListOf<MutableList<MutableList<Int>>>(
        mutableListOf<MutableList<Int>>(
            mutableListOf<Int>(0,0,0,0),
            mutableListOf<Int>(0, 0, 0),
            mutableListOf<Int>(0, 0)
        ),
        mutableListOf<MutableList<Int>>(
            mutableListOf<Int>(0,0,0,0),
            mutableListOf<Int>(0, 0, 0),
            mutableListOf<Int>(0, 0)
        )
    )

    suspend fun getBoardState(): MutableList<MutableList<MutableList<Int>>> {
        return this.boardState
    }
    suspend fun update(playerIndex: Int, row: Int, column: Int, value: Int) {
        this.boardState[playerIndex][row][column] = value
    }
    suspend fun remove(playerIndex: Int, row: Int, column: Int) {
        this.boardState[playerIndex][row][column] = 0
    }
}
