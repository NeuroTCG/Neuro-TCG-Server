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

    /*
    ClientInfoPacket("Official Client", "0.0.1", 1u)
    ClientInfoAcceptPacket()
    DisconnectPacket(
        DisconnectPacket.Reason.protocol_too_old,
        "Your game is outdated. Please update it or inform your client maintainer"
    )
    AuthenticatePacket("Neuro")
    AuthenticationValidPacket(false)
    MatchFoundPacket(UserInfo("Evil", "Vedals PC"), UUID.randomUUID(), false)

    UnknownPacketPacket(17)

    GetGameStatePacket(GetGameStatePacket.Reason.reconnect)
    val summon = SummonPacket(3, BoardPosition(1, 2))
    summon.getResponsePacket(true, FullCardState(3, 100))
    summon.getOpponentPacket(FullCardState(3, 100))

    val attack = AttackPacket(BoardPosition(1, 1), BoardPosition(0, 2))
    attack.getResponsePacket(true, FullCardState(1, 29), FullCardState(3, 90))
    attack.getOpponentPacket(FullCardState(1, 29), FullCardState(3, 90))

    exitProcess(0)
    */

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
