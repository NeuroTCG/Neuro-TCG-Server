package objects

import okio.withLock
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock

class MatchmakingResult(
    val game: Game,
    val player: Player,
)

class MatchmakingQueue(
    val db: GameDatabase,
) {
    private val queue: Queue<Pair<GameConnection, CompletableFuture<MatchmakingResult>>> = LinkedList()
    private val lock = ReentrantLock()

    fun addPlayerIfNotInQueueOrGame(connection: GameConnection): CompletableFuture<MatchmakingResult>? {
        lock.withLock {
            if (isPlayerInQueue(connection.getUserInfo().id)) {
                return null
            } else if (db.userGetCurrentGame(connection.getUserInfo().id) != null) {
                // this check is only race condition free because the only way to create a
                // game is matchmakeEveryone() and that also holds the lock while
                // doing so.
                return null
            } else {
                val gameFuture = CompletableFuture<MatchmakingResult>()
                queue.add(Pair(connection, gameFuture))
                return gameFuture
            }
        }
    }

    private fun tryGetFirstOpenConnection(): Pair<GameConnection, CompletableFuture<MatchmakingResult>>? {
        lock.withLock {
            while (queue.isNotEmpty()) {
                val (connection, mmr) = queue.remove()
                if (connection.isOpen) {
                    return Pair(connection, mmr)
                }
            }
        }
        return null
    }

    suspend fun matchmakeEveryone() {
        lock.withLock {
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

    private fun removeDisconnectedPlayers() {
        lock.withLock {
            queue.retainAll { (c, _) -> c.isOpen }
        }
    }

    private fun isPlayerInQueue(id: TcgId): Boolean {
        lock.withLock {
            removeDisconnectedPlayers()

            return queue.any { (connection, _) ->
                connection.getUserInfo().id == id
            }
        }
    }
}
