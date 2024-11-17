package objects.accounts

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*

class DiscordLoginProvider(
    private val redirectUrl: String,
    private val clientId: String,
    private val clientSecret: String
) : LoginProvider {
    private val results: MutableMap<String, CompletableDeferred<LoginProviderResult>> = mutableMapOf()

    override fun name(): String {
        return "discord"
    }

    override suspend fun handleInitialRequest(correlationId: String, call: ApplicationCall) {
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
        builder.parameters.append("client_id", this.clientId);
        builder.parameters.append("redirect_uri", redirectUrl);
        builder.parameters.append("scope", "identify");
        builder.parameters.append("state", correlationId);

        return builder.toString()
    }

    fun handleRedirect(code: String, state: String) {
        val deferred = results[state] ?: return

        // TODO: handle failure here? (e.g. invalid code)
        val tcgUserId = getTcgUserFromOauthCodeResponse(code);

        deferred.complete(LoginSuccess(tcgUserId))
    }

    // TODO: actually fetch from discord here, using https://discord.com/api/oauth2/token
    private fun getTcgUserFromOauthCodeResponse(code: String): String {
        return "DUMMY USER ID"
    }
}
