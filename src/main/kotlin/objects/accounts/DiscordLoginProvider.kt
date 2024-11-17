package objects.accounts

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

class DiscordLoginProvider (
    private val redirectUrl: String,
    private val clientId: String,
    private val clientSecret: String
): LoginProvider {
    private val channels: MutableMap<String, CompletableDeferred<LoginProviderResult>> = mutableMapOf()

    override fun name(): String {
        return "discord"
    }

    override fun registerRoutes(route: Route) {
        route.get("/begin") {
            // NOTE: if `correlationId` does not exist, then this returns a 406 not acceptable, I think it's a ktor thing?
            val correlationId = call.request.queryParameters["correlationId"]!!

            call.respondRedirect(getFlowStartUrl(correlationId))
        }

        println("REMOVE ME: $route")
    }
    override suspend fun waitForLogin(correlationId: String): LoginProviderResult {
        val deferred = CompletableDeferred<LoginProviderResult>()
        channels[correlationId] =  deferred

        val result = deferred.await()
        channels.remove(correlationId)
        return result
    }

    fun getFlowStartUrl(correlationId: String): String {
        val builder = URLBuilder("https://discordapp.com/oauth2/authorize")
        builder.parameters.append("response_type", "code")
        builder.parameters.append("client_id", this.clientId);
        builder.parameters.append("redirect_uri", redirectUrl);
        builder.parameters.append("scope", "identify");
        builder.parameters.append("state", correlationId);

        return builder.toString()
    }

    fun handleRedirect(code: String, state: String) {
        val deferred = channels[state] ?: return

        // TODO: handle failure here? (e.g. invalid code)
        val tcgUserId = getTcgUserFromOauthCodeResponse(code);

        deferred.complete(LoginSuccess(tcgUserId))
    }

    // TODO: actually fetch from discord here, using https://discord.com/api/oauth2/token
    private fun getTcgUserFromOauthCodeResponse(code: String): String {
        return "DUMMY USER ID"
    }
}
