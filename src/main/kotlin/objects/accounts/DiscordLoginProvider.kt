package objects.accounts

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import objects.*
import java.util.concurrent.*
import kotlin.collections.set

class DiscordLoginProvider(
    private val redirectUrl: String,
    private val clientId: String,
    private val clientSecret: String,
    private val db: GameDatabase,
) : LoginProvider {
    private val results: ConcurrentMap<CorrelationId, CompletableDeferred<LoginProviderResult>> = ConcurrentHashMap()

    @OptIn(ExperimentalSerializationApi::class)
    private val httpClient =
        HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                }
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        namingStrategy = JsonNamingStrategy.SnakeCase
                        ignoreUnknownKeys = true
                    },
                )
            }
        }

    override val name: String = "discord"

    override suspend fun handleInitialRequest(
        correlationId: CorrelationId,
        call: ApplicationCall,
    ) {
        results[correlationId] = CompletableDeferred()
        call.respondRedirect(getFlowStartUrl(correlationId))
    }

    override fun registerAdditionalRoutes(route: Route) {
        route.get("/redirect") {
            handleRedirect(call.request.queryParameters["code"]!!, call.request.queryParameters["state"]!!)

            call.respondRedirect("/auth/safe_to_close")
        }
    }

    override suspend fun waitForLogin(correlationId: CorrelationId): LoginProviderResult? {
        val deferred = results[correlationId] ?: return null

        val result = deferred.await()
        results.remove(correlationId)
        return result
    }

    private fun getFlowStartUrl(correlationId: CorrelationId): String {
        val builder = URLBuilder("https://discordapp.com/oauth2/authorize")
        builder.parameters.append("response_type", "code")
        builder.parameters.append("client_id", this.clientId)
        builder.parameters.append("redirect_uri", redirectUrl)
        builder.parameters.append("scope", "identify")
        builder.parameters.append("state", correlationId.value)

        return builder.toString()
    }

    private suspend fun handleRedirect(
        code: String,
        state: String,
    ) {
        val deferred = results[CorrelationId(state)] ?: return

        try {
            val tcgUserId = getTcgUserFromOauthCodeResponse(code)
            deferred.complete(LoginSuccess(tcgUserId))
        } catch (e: Exception) {
            deferred.complete(LoginException(e))
        }
    }

    private suspend fun getTcgUserFromOauthCodeResponse(code: String): TcgId {
        val url = URLBuilder("https://discord.com/api/oauth2/token")
        url.parameters.append("client_id", this.clientId)
        val response: DiscordOauthTokenResponse =
            httpClient
                .post(url.build(), {
                    contentType(ContentType.Application.FormUrlEncoded)
                    basicAuth(clientId, clientSecret)
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("grant_type", "authorization_code")
                                append("code", code)
                                append("redirect_uri", redirectUrl)
                            },
                        ),
                    )
                })
                .body()

        val discordUserInfo: DiscordOauthUserInfo =
            httpClient
                .get("https://discord.com/api/v10/users/@me") {
                    headers {
                        append("Authorization", "${response.tokenType} ${response.accessToken}")
                    }
                }.body()

        var tcgUserId = db.getUserByDiscordId(DiscordId(discordUserInfo.id))

        if (tcgUserId == null) {
            tcgUserId = db.createNewUser()
            db.createLinkedDiscordInfo(discordUserInfo, response, tcgUserId)
        }

        db.updateDiscordUserInfo(discordUserInfo, response)

        return tcgUserId
    }

    @Serializable
    class DiscordOauthTokenResponse(
        val accessToken: String,
        val tokenType: String,
        val expiresIn: ULong,
        val refreshToken: String,
        val scope: String,
    )

    @Serializable
    class DiscordOauthUserInfo(
        val username: String,
        val id: String,
    )
}
