import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.future.*
import objects.*
import java.io.*
import java.util.*
import java.util.concurrent.*

fun main() {
    val db = GameDatabase()
    if (!File("data/data.db").exists()) {
        db.createTables()
    }

    val player_queue: Queue<Pair<GameConnection, CompletableFuture<Pair<Game, Boolean>>>> = LinkedList()

    println("Listening for clients...")
    embeddedServer(Netty, port = 9933) {
        install(WebSockets)
        routing {
            webSocket("/game") {
                println("New connection established")
                val connection = GameConnection(this)
                connection.connect()
                val gameFuture = CompletableFuture<Pair<Game, Boolean>>()
                player_queue.add(Pair(connection, gameFuture))

                while (player_queue.count() >= 2) {
                    val (p1c, p1gf) = player_queue.remove()
                    val (p2c, p2gf) = player_queue.remove()
                    val new_game = Game(p1c, p2c, db)
                    println("Starting game ${new_game.id}")
                    p1gf.complete(Pair(new_game, true))
                    p2gf.complete(Pair(new_game, false))

                    println("Game finished")
                }

                println("Waiting for opponent")
                try {
                    val (game, isFirstPlayer) = gameFuture.await()
                    game.mainLoop(isFirstPlayer)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                println("Game finished")
            }
        }
    }.start(wait = true)
}
