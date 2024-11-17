package objects.accounts

import com.google.gson.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import objects.*
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.collections.set

class DiscordLoginProvider(
    private val redirectUrl: String,
    private val clientId: String,
    private val clientSecret: String,
    private val db: GameDatabase,
) : LoginProvider {
    private val results: MutableMap<String, CompletableDeferred<LoginProviderResult>> = mutableMapOf()

    private val httpClient =
        HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                }
            }

            install(ContentNegotiation) {
                gson {
                    setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                }
            }
        }

    override fun name(): String = "discord"

    override suspend fun handleInitialRequest(
        correlationId: String,
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

    override suspend fun waitForLogin(correlationId: String): LoginProviderResult {
        val deferred = results[correlationId]!!

        val result = deferred.await()
        results.remove(correlationId)
        return result
    }

    private fun getFlowStartUrl(correlationId: String): String {
        val builder = URLBuilder("https://discordapp.com/oauth2/authorize")
        builder.parameters.append("response_type", "code")
        builder.parameters.append("client_id", this.clientId)
        builder.parameters.append("redirect_uri", redirectUrl)
        builder.parameters.append("scope", "identify")
        builder.parameters.append("state", correlationId)

        return builder.toString()
    }

    suspend fun handleRedirect(
        code: String,
        state: String,
    ) {
        val deferred = results[state] ?: return

        try {
            val tcgUserId = getTcgUserFromOauthCodeResponse(code)
            deferred.complete(LoginSuccess(tcgUserId))
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
        }
    }

    private suspend fun getTcgUserFromOauthCodeResponse(code: String): String {
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
        println(response)

        val discordUserInfo: DiscordOauthUserInfo =
            httpClient
                .get("https://discord.com/api/v10/users/@me") {
                    headers {
                        append("Authorization", "${response.tokenType} ${response.accessToken}")
                    }
                }.body()
        println(discordUserInfo)

        var tcgUserId = db.getUserByDiscordId(discordUserInfo.id)

        if (tcgUserId == null) {
            tcgUserId = db.createNewUser()
        }

        db.updateDiscordUserInfo(discordUserInfo, tcgUserId)

        return tcgUserId
    }

    private class DiscordOauthTokenRequest(
        val grantType: String,
        val code: String,
        val redirectUri: String,
    )

    private class DiscordOauthTokenResponse(
        val accessToken: String,
        val tokenType: String,
        val expiresIn: ULong,
        val refreshToken: String,
        val scope: String,
    )

    private class DiscordOauthUserInfo(
        val username: String,
        val id: String,
    )
}
