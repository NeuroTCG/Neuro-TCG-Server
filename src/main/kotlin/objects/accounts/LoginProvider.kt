package objects.accounts

import io.ktor.server.routing.*

interface LoginProvider {
    fun name(): String
    fun registerRoutes(route: Route)
    suspend fun waitForLogin(correlationId: String): LoginProviderResult
}
