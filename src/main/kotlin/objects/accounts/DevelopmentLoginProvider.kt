package objects.accounts

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.*
import objects.*
import java.util.concurrent.*

class DevelopmentLoginProvider(
    private val db: GameDatabase,
) : LoginProvider {
    private val results: ConcurrentHashMap<CorrelationId, CompletableDeferred<LoginProviderResult>> = ConcurrentHashMap()

    override val name = "__development"

    override suspend fun handleInitialRequest(
        correlationId: CorrelationId,
        call: ApplicationCall,
    ) {
        val auth = call.request.authorization() ?: return call.response.status(HttpStatusCode.Unauthorized)

        val callerId =
            db.getUserIdFromToken(
                Token(auth.removePrefix("Bearer ")),
            ) ?: return call.response.status(HttpStatusCode.Unauthorized)

        if (!db.userHasFlag(callerId, Flag("is_developer"))!!) {
            return call.response.status(HttpStatusCode.Forbidden)
        }

        val devUserId = DevelopmentId(call.request.queryParameters["devUserId"]!!)
        var devAccountTcgId = db.getUserByDevelopmentId(devUserId, callerId)

        if (devAccountTcgId == null) {
            devAccountTcgId = db.createNewUser()
            db.createLinkedDevelopmentInfo(devUserId, callerId, devAccountTcgId)
        }

        results[correlationId] = CompletableDeferred(LoginSuccess(devAccountTcgId))
        call.response.status(HttpStatusCode.OK)
    }

    override suspend fun waitForLogin(correlationId: CorrelationId): LoginProviderResult? {
        val result = results[correlationId]?.await()
        results.remove(correlationId)
        return result
    }
}
