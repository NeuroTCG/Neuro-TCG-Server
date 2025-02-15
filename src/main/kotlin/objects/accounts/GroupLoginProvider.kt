package objects.accounts

import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.*

class GroupLoginProvider(
    private val webserverBase: String,
    val providers: List<LoginProvider>,
) {
    private val results: ConcurrentMap<CorrelationId, CompletableDeferred<LoginProviderResult>> = ConcurrentHashMap()

    fun beginAuth(): BeginLoginInfo {
        val correlationId = generateCorrelationId()

        val userLoginUrl = URLBuilder("$webserverBase/auth/login")
        userLoginUrl.parameters.append("correlationId", correlationId.value)

        val pollUrl = URLBuilder("$webserverBase/auth/poll")
        pollUrl.parameters.append("correlationId", correlationId.value)

        return BeginLoginInfo(
            correlationId,
            userLoginUrl.build().toString(),
            pollUrl.build().toString(),
        )
    }

    suspend fun waitForLogin(correlationId: CorrelationId): LoginProviderResult? = results[correlationId]?.await()

    fun setResult(
        correlationId: CorrelationId,
        result: LoginProviderResult,
    ) {
        results[correlationId]?.complete(result)
    }

    suspend fun timeoutLogin(correlationId: CorrelationId) {
        // give users (up to) 20 minutes to log in
        delay(1000 * 60 * 20)
        results[correlationId]?.complete(LoginFailure("timeout"))
    }

    private fun generateCorrelationId(): CorrelationId {
        val id = CorrelationId(UUID.randomUUID().toString())

        assert(!results.containsKey(id)) { "id generated already exists" }

        results[id] = CompletableDeferred()

        return id
    }

    fun isValidCorrelation(correlationId: CorrelationId): Boolean = results.containsKey(correlationId)
}

@Serializable
class BeginLoginInfo(
    val correlationId: CorrelationId,
    val userLoginUrl: String,
    val pollUrl: String,
)

@JvmInline
@Serializable
value class CorrelationId(
    val value: String,
)
