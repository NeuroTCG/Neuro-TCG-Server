package objects.accounts

import io.ktor.http.*
import kotlinx.coroutines.*
import java.util.UUID

class GroupLoginProvider(
    private val providers: List<LoginProvider>,
) {
    // This should probably be using a thread-safe data structure given there's coroutines?
    private val results: MutableMap<String, CompletableDeferred<LoginProviderResult>> = mutableMapOf()

    fun providers(): List<LoginProvider> = providers

    fun beginAuth(): BeginLoginInfo {
        val correlationId = generateCorrelationId()

        // TODO: These two URLs should not be hardcoded to localhost. We should probably pass in some info
        val userLoginUrl = URLBuilder("http://localhost:9933/auth/login")
        userLoginUrl.parameters.append("correlationId", correlationId)

        val pollUrl = URLBuilder("http://localhost:9933/auth/poll")
        pollUrl.parameters.append("correlationId", correlationId)

        return BeginLoginInfo(
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

    private fun generateCorrelationId(): String {
        val id = UUID.randomUUID().toString()

        assert(!results.containsKey(id), { "id generated already exists" })

        results[id] = CompletableDeferred()

        return id
    }

    fun isValidCorrelation(correlationId: String): Boolean = results.containsKey(correlationId)
}

class BeginLoginInfo(
    val userLoginUrl: String,
    val pollUrl: String,
)
