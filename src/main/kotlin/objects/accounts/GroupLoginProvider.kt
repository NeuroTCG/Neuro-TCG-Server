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
        val correlationId = generateCorrelationId();

        val userLoginUrl = URLBuilder("http://localhost:9934/auth/login")
        userLoginUrl.parameters.append("correlationId", correlationId)

        val pollUrl = URLBuilder("http://localhost:9934/auth/poll")
        pollUrl.parameters.append("correlationId", correlationId)

        return BeginLoginInfo(
            userLoginUrl.build().toString(),
            pollUrl.build().toString(),
        )
    }

    suspend fun waitForLogin(correlationId: String): LoginProviderResult? {
        return results[correlationId]?.await()
    }

    fun setResult(correlationId: String, result: LoginProviderResult) {
        results[correlationId]?.complete(result)
    }

    // TODO: make this return an actual correlation id (any random unique value)
    private fun generateCorrelationId(): String {
        val id = UUID.randomUUID().toString()

        assert(!results.containsKey(id), { "id generated already exists" })

        results[id] = CompletableDeferred()

        return id
    }

    fun isValidCorrelation(correlationId: String): Boolean {
        return results[correlationId] != null
    }
}

class BeginLoginInfo(val userLoginUrl: String, val pollUrl: String)
