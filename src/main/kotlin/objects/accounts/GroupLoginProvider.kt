package objects.accounts

import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.*

class GroupLoginProvider(
    private val webserverBase: String,
    private val providers: List<LoginProvider>,
) {
    private val results: ConcurrentMap<String, CompletableDeferred<LoginProviderResult>> = ConcurrentHashMap()

    fun providers(): List<LoginProvider> = providers

    fun beginAuth(): BeginLoginInfo {
        val correlationId = generateCorrelationId()

        val userLoginUrl = URLBuilder("$webserverBase/auth/login")
        userLoginUrl.parameters.append("correlationId", correlationId)

        val pollUrl = URLBuilder("$webserverBase/auth/poll")
        pollUrl.parameters.append("correlationId", correlationId)

        return BeginLoginInfo(
            correlationId,
            userLoginUrl.build().toString(),
            pollUrl.build().toString(),
        )
    }

    suspend fun waitForLogin(correlationId: String): LoginProviderResult? = results[correlationId]?.await()

    fun setResult(
        correlationId: String,
        result: LoginProviderResult,
    ) {
        results[correlationId]?.complete(result)
    }

    suspend fun timeoutLogin(correlationId: String) {
        // give users (up to) 20 minutes to log in
        delay(1000 * 60 * 20)
        results[correlationId]?.complete(LoginFailure("timeout"))
    }

    private fun generateCorrelationId(): String {
        val id = UUID.randomUUID().toString()

        assert(!results.containsKey(id)) { "id generated already exists" }

        results[id] = CompletableDeferred()

        return id
    }

    fun isValidCorrelation(correlationId: String): Boolean = results.containsKey(correlationId)
}

@Serializable
class BeginLoginInfo(
    val correlationId: String,
    val userLoginUrl: String,
    val pollUrl: String,
)
