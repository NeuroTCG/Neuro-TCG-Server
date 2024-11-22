import io.github.cdimascio.dotenv.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.pebble.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.pebbletemplates.pebble.loader.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import objects.*
import objects.accounts.*
import java.io.*
import java.util.*
import java.util.concurrent.*

fun getFirstOpenConnection(
    queue: Queue<Pair<GameConnection, CompletableFuture<Pair<Game, Player>>>>,
): Pair<GameConnection, CompletableFuture<Pair<Game, Player>>>? {
    while (queue.isNotEmpty()) {
        val (c, gf) = queue.remove()
        if (c.isOpen) {
            return Pair(c, gf)
        }
    }
    return null
}

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val db = GameDatabase()
    if (!File("data/data.db").exists()) {
        db.createTables()
    }

    val playerQueue: Queue<Pair<GameConnection, CompletableFuture<Pair<Game, Player>>>> =
        LinkedList()

    println("Listening for clients...")
    val dotenv = dotenv()

    val webserverBase = dotenv["WEB_HOST_BASE"]

    val groupLoginProvider =
        GroupLoginProvider(
            webserverBase,
            listOf(
                DiscordLoginProvider(
                    "$webserverBase/auth/providers/discord/redirect",
                    dotenv["DISCORD_CLIENT_ID"],
                    dotenv["DISCORD_CLIENT_SECRET"],
                    db,
                ),
            ),
        )

    embeddedServer(Netty, 9933) {
        install(ContentNegotiation) {
            json(
                Json {
                    namingStrategy = JsonNamingStrategy.SnakeCase
                },
            )
        }

        install(Pebble) {
            loader(
                FileLoader().apply {
                    prefix = "templates/"
                    suffix = ".pebble"
                },
            )
        }

        install(WebSockets)

        routing {
            route("/auth") {
                post("/begin") {
                    val authInfo = groupLoginProvider.beginAuth()

                    launch {
                        groupLoginProvider.timeoutLogin(authInfo.correlationId)
                    }

                    call.respond(authInfo)
                }

                get("/login") {
                    val correlationId = CorrelationId(call.request.queryParameters["correlationId"]!!)

                    if (!groupLoginProvider.isValidCorrelation(correlationId)) {
                        call.respond(HttpStatusCode.BadRequest, "invalid correlationId, please try logging in again")
                    }

                    val providers: List<Map<String, String>> =
                        groupLoginProvider.providers.map {
                            val url = URLBuilder("$webserverBase/auth/providers/${it.name}/begin")
                            url.parameters.append("correlationId", correlationId.value)
                            mapOf(
                                "name" to it.name,
                                "redirect" to url.buildString(),
                            )
                        }

                    call.respond(PebbleContent("loginChoices", mapOf("providers" to providers)))
                }

                get("/poll") {
                    val result = groupLoginProvider.waitForLogin(CorrelationId(call.request.queryParameters["correlationId"]!!))

                    when (result) {
                        is LoginSuccess -> {
                            val token = db.generateTokenFor(result.userId)
                            call.respondText(token!!, ContentType.Text.Plain, HttpStatusCode.OK)
                        }

                        is LoginFailure -> {
                            println("had login failure: ${result.reason}")
                            call.respond(HttpStatusCode.BadRequest)
                        }

                        else -> {
                            println("got unexpected login result: $result")
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    }
                }

                get("/safe_to_close") {
                    call.respondText {
                        "Logged in. You can close this page."
                    }
                }

                for (provider in groupLoginProvider.providers) {
                    route("/providers/${provider.name}") {
                        println("registering $provider under $this")
                        get("/begin") {
                            val correlationId = CorrelationId(call.request.queryParameters["correlationId"]!!)

                            if (!groupLoginProvider.isValidCorrelation(correlationId)) {
                                call.respond(HttpStatusCode.BadRequest)
                                return@get
                            }

                            provider.handleInitialRequest(correlationId, call)

                            launch {
                                val result = provider.waitForLogin(correlationId) ?: return@launch

                                groupLoginProvider.setResult(correlationId, result)
                            }
                        }

                        provider.registerAdditionalRoutes(this)
                    }
                }
            }

            route("/users") {
                get("/@me") {
                    val auth = call.request.authorization()

                    if (auth == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@get
                    }

                    val mappedUser = db.getUserIdFromToken(Token(auth.removePrefix("Bearer ")))

                    if (mappedUser == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@get
                    }

                    @Serializable
                    class UserInfo(
                        val userId: TcgId,
                    )

                    call.respond(UserInfo(mappedUser))
                }
            }

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
