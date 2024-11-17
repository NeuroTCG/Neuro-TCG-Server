package objects.accounts

import io.ktor.http.*
import kotlinx.coroutines.*

class GroupLoginProvider(
    private val providers: List<LoginProvider>,
) {
    // This should probably be using a thread-safe data structure given there's coroutines?
    private val deferredCompletables: MutableMap<String, CompletableDeferred<LoginProviderResult>> = mutableMapOf()

    fun providers(): List<LoginProvider> = providers

    fun beginAuth(): BeginLoginInfo {
        val userLoginUrl = URLBuilder("http://localhost:9934/auth/login")
        userLoginUrl.parameters.append("correlationId", generateCorrelationId())

        val pollUrl = URLBuilder("http://localhost:9934/auth/poll")
        pollUrl.parameters.append("correlationId", generateCorrelationId())

        return BeginLoginInfo(
            userLoginUrl.build().toString(),
            pollUrl.build().toString(),
        )
    }

    suspend fun waitForLogin(correlationId: String): LoginProviderResult? {
        return deferredCompletables[correlationId]?.await()
    }

    fun loginCompleted(correlationId: String, result: LoginProviderResult) {
        deferredCompletables[correlationId]?.complete(result)
    }

    // TODO: make this return an actual correlation id (any random unique value)
    fun generateCorrelationId(): String {
        return "dummy correlation id"
    }
}

class BeginLoginInfo(val userLoginUrl: String, val pollUrl: String)
