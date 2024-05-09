import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import objects.*
import objects.shared.*

fun main() {
    CardStats.cardIDMapping = hashMapOf(
        Pair(0, CardStats(100, 50)),
        Pair(1, CardStats(200, 5)),
    )

    val db = GameDatabase()
    db.createTables()

    println("Listening for clients...")
    embeddedServer(Netty, port = 9933) {
        install(WebSockets)
        routing {
            webSocket("/game") {
                println("New connection established")
                val game = Game(this, db)
                println("Starting game ${game.id}")
                game.mainLoop()
                println("Game finished")
            }
        }
    }.start(wait = true)
}
