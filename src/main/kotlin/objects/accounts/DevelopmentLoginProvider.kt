package objects.accounts

import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.coroutines.*
import objects.*
import java.util.concurrent.*

class DevelopmentLoginProvider(
    private val devToken: String,
    private val db: GameDatabase,
) : LoginProvider {
    private val results: ConcurrentHashMap<CorrelationId, CompletableDeferred<LoginProviderResult>> = ConcurrentHashMap()

    override val name = "__development"

    override suspend fun handleInitialRequest(
        correlationId: CorrelationId,
        call: ApplicationCall,
    ) {
        val devToken = call.request.queryParameters["devToken"]!!
        val devUserId = DevelopmentId(call.request.queryParameters["devUserId"]!!)

        if (devToken != this.devToken) {
            return call.response.status(HttpStatusCode.Forbidden)
        }

        var userId = db.getUserByDevelopmentId(devUserId)

        if (userId == null) {
            userId = db.createNewUser()
            db.createLinkedDevelopmentInfo(devUserId, userId)
        }

        results[correlationId] = CompletableDeferred(LoginSuccess(userId))
        call.response.status(HttpStatusCode.OK)
    }

    override suspend fun waitForLogin(correlationId: CorrelationId): LoginProviderResult? {
        val result = results[correlationId]?.await()
        results.remove(correlationId)
        return result
    }
}
