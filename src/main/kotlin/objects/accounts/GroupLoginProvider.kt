package objects.accounts

import io.ktor.http.*

class GroupLoginProvider(
    private val providers: List<LoginProvider>,
) {
    public fun providers(): List<LoginProvider> = providers

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

    suspend fun waitForLogin(correlationId: String): LoginProviderResult {
        TODO("Do login stuff")
    }

    // TODO: make this return an actual correlation id (any random unique value)
    fun generateCorrelationId(): String {
        return "dummy correlation id"
    }
}

class BeginLoginInfo(val userLoginUrl: String, val pollUrl: String)
