import io.github.cdimascio.dotenv.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.* import io.ktor.server.websocket.*
import kotlinx.coroutines.future.*
import objects.*
import objects.accounts.*
import objects.packets.objects.CardStats
import java.io.*
import java.util.*
import java.util.concurrent.*

fun getFirstOpenConnection(
    queue: Queue<Pair<GameConnection, CompletableFuture<Pair<Game, Player>>>>,
): Pair<GameConnection, CompletableFuture<Pair<Game, Player>>>? {
    while (queue.count() >= 1) {
        val (c, gf) = queue.remove()
        if (c.isOpen) {
            return Pair(c, gf)
        }
    }
    return null
}

fun warnForDebugSettings() {
    val warn = { msg: String ->
        println("\u001b[0;33m" + msg + "\u001b[0m")
    }

    if (Game.DEBUG_EVENTS_ENABLED) {
        warn("WARNING: Debug events are enabled!")
    }
    if (CardStats.FREE_EVERYTHING) {
        warn("WARNING: Zero cost cards are enabled!")
    }
}


val dotenv = dotenv() // usage: dotenv[key: String]
val discordLoginManager = DiscordLogin(dotenv["DISCORD_CLIENT_SECRET"]!!, dotenv["DISCORD_CLIENT_ID"]!!, dotenv["DISCORD_REDIRECT_URI"]!!)
// TODO: add .env file with secrets

fun main() {
    val db = GameDatabase()
    if (!File("data/data.db").exists()) {
        db.createTables()
    }

    val playerQueue: Queue<Pair<GameConnection, CompletableFuture<Pair<Game, Player>>>> =
        LinkedList()

    warnForDebugSettings()

    println("Listening for clients...")
    embeddedServer(Netty, port = 9933) {
        install(WebSockets)
        routing {
            webSocket("/game") {
                println("New connection established")
                val connection = GameConnection(this)
                connection.connect()
                val gameFuture = CompletableFuture<Pair<Game, Player>>()
                playerQueue.add(Pair(connection, gameFuture))

                while (playerQueue.count() >= 2) {
                    val (p1c, p1gf) = getFirstOpenConnection(playerQueue) ?: break
                    val p2 = getFirstOpenConnection(playerQueue)
                    if (p2 == null) {
                        playerQueue.add(Pair(p1c, p1gf))
                        break
                    }
                    val (p2c, p2gf) = p2

                    val newGame = Game(p1c, p2c, db)
                    println("Starting game ${newGame.id}")
                    p1gf.complete(Pair(newGame, Player.Player1))
                    p2gf.complete(Pair(newGame, Player.Player2))

                    println("Game finished")
                }

                println("Waiting for opponent")
                try {
                    val (game, player) = gameFuture.await()
                    game.mainLoop(player)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                println("Game finished")
            }
        }
    }.start(wait = true)
}
