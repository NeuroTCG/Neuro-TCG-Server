package objects

import java.util.*
import java.util.concurrent.*

class MatchmakingResult(
    val game: Game,
    val player: Player,
)

class MatchmakingQueue {
    private val queue: Queue<Pair<GameConnection, CompletableFuture<MatchmakingResult>>> = LinkedList()

    fun tryGetFirstOpenConnection(): Pair<GameConnection, CompletableFuture<MatchmakingResult>>? {
        while (queue.isNotEmpty()) {
            val (connection, mmr) = queue.remove()
            if (connection.isOpen) {
                return Pair(connection, mmr)
            }
        }
        return null
    }

    fun addPlayer(connection: GameConnection): CompletableFuture<MatchmakingResult> {
        val gameFuture = CompletableFuture<MatchmakingResult>()

        queue.add(Pair(connection, gameFuture))

        return gameFuture
    }

    suspend fun matchmakeEveryone(db: GameDatabase) {
        while (queue.count() >= 2) {
            val (p1c, p1gf) = tryGetFirstOpenConnection() ?: break
            val p2 = tryGetFirstOpenConnection()

            if (p2 == null) {
                queue.add(Pair(p1c, p1gf))
                break
            }
            val (p2c, p2gf) = p2

            val newGame = Game(p1c, p2c, db)
            println("Created game ${newGame.id}")
            p1gf.complete(MatchmakingResult(newGame, Player.Player1))
            p2gf.complete(MatchmakingResult(newGame, Player.Player2))
        }
    }
}
