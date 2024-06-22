import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import objects.*
import java.io.File

fun main() {
    val db = GameDatabase()
    if (!File("data/data.db").exists()) {
        db.createTables()
    }

    println("Listening for clients...")
    embeddedServer(Netty, port = 9933) {
        install(WebSockets)
        routing {
            webSocket("/game") {
                println("New connection established")
                try {
                    val game = Game(this, db)
                    println("Starting game ${game.id}")
                    game.mainLoop()
                } catch (e: Exception){
                    e.printStackTrace()
                }
                println("Game finished")
            }
        }
    }.start(wait = true)
}
