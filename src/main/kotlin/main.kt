import io.github.cdimascio.dotenv.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.pebble.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.pebbletemplates.pebble.loader.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import objects.*
import objects.accounts.*
import objects.packets.*
import objects.packets.objects.UserInfo
import java.util.*
import java.util.concurrent.*

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val dotenv = dotenv()

    val dbPath = "data/data.db"
    val db = GameDatabase(dbPath)
    db.createTables()

    val playerQueue = MatchmakingQueue(db)

    println("Listening for clients...")

    val webserverBase = dotenv["WEB_HOST_BASE"]

    val groupLoginProvider =
        GroupLoginProvider(
            webserverBase,
            listOf(
                DiscordLoginProvider(
                    webserverBase,
                    "$webserverBase/auth/providers/discord/redirect",
                    dotenv["DISCORD_CLIENT_ID"],
                    dotenv["DISCORD_CLIENT_SECRET"],
                    db,
                ),
                DevelopmentLoginProvider(db),
            ),
        )

    val adminAuth = AdminAuth(db)

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

        install(CORS) {
            allowSameOrigin

            allowHost("0.0.0.0:8060") // the godot one-click-deploy server runs here
            allowHost("neurotcg.github.io")

            allowHeaders { true } // all headers

            HttpMethod.DefaultMethods.forEach { allowMethod(it) } // all methods
        }

        install(Authentication) {
            bearer("user") {
                authenticate { tokenCredential -> db.getUserIdFromToken(Token(tokenCredential.token)) }
            }

            bearer("admin") {
                authenticate { tokenCredential -> adminAuth.authenticateToken(Token(tokenCredential.token)) }
            }
        }

        install(WebSockets)

        routing {
            get("/fire") {
                call.respond("water")
            }

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

                    // TODO: ideally there'd be a way to get DevelopmentLoginProvider's name statically
                    val providers: List<Map<String, String>> =
                        groupLoginProvider.providers.filter { it.name != AuthProviderName("__development") }.map {
                            val url = URLBuilder("$webserverBase/auth/providers/${it.name.name}/begin")
                            url.parameters.append("correlationId", correlationId.value)
                            mapOf(
                                "name" to it.name.name,
                                "redirect" to url.buildString(),
                            )
                        }

                    call.respond(PebbleContent("loginChoices", mapOf("providers" to providers)))
                }

                get("/poll") {
                    val result =
                        groupLoginProvider.waitForLogin(CorrelationId(call.request.queryParameters["correlationId"]!!))

                    when (result) {
                        is LoginSuccess -> {
                            val token = db.generateTokenFor(result.userId)
                            call.respondText(token!!.token, ContentType.Text.Plain, HttpStatusCode.OK)
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
                    route("/providers/${provider.name.name}") {
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
            authenticate("user") {
                route("/users") {
                    get("/@me") {
                        val mappedUserId = call.principal<TcgId>()!!

                        call.respond(UserInfo(mappedUserId))
                    }
                }
            }

            webSocket("/game") connectionHandler@{
                println("New connection established")
                val connection = GameConnection(this, db)
                connection.connectAndAuthenticate(playerQueue)
                if (!connection.isOpen) {
                    connection.waitForClose()
                    return@connectionHandler
                }

                val gameFuture = playerQueue.addPlayerIfNotInQueueOrGame(connection)

                if (gameFuture == null) {
                    connection.sendPacket(
                        DisconnectPacket(DisconnectPacket.Reason.double_login, "You are already in the queue or in a game"),
                    )
                    connection.waitForClose()
                    return@connectionHandler
                }

                playerQueue.matchmakeEveryone()

                println("Waiting for opponent")
                val match = gameFuture.await()
                try {
                    match.game.mainLoop(match.player)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (match.player == Player.Player1) {
                        db.moveGameToArchive(match.game.id)
                    }
                }
                println("Game finished")
                connection.waitForClose()
            }

            authenticate("admin") {
                route("/admin") {
                    // authentication should probably be done by some kind of middleware in here?

                    route("/users") {
                        route("/{userId}") {
                            get {
                                // get user info
                            }

                            route("/flags") {
                                get {
                                    call.respond(db.userListFlags(TcgId(call.parameters["userId"]!!)))
                                }

                                route("/{flag}") {
                                    get {
                                        val has =
                                            db.userHasFlag(TcgId(call.parameters["userId"]!!), Flag(call.parameters["flag"]!!))
                                                ?: return@get call.respond(HttpStatusCode.NoContent)

                                        call.respond(has)
                                    }

                                    post {
                                        db.userSetFlag(TcgId(call.parameters["userId"]!!), Flag(call.parameters["flag"]!!))
                                        call.respond(HttpStatusCode.OK)
                                    }

                                    delete {
                                        db.userUnsetFlag(TcgId(call.parameters["userId"]!!), Flag(call.parameters["flag"]!!))
                                        call.respond(HttpStatusCode.OK)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}
