package objects.accounts

import io.ktor.server.application.*
import io.ktor.server.routing.*

interface LoginProvider {
    fun name(): String

    fun registerAdditionalRoutes(route: Route) {  }

    // This should handle the initial provider request, and internally acknowledge the correlation id
    suspend fun handleInitialRequest(correlationId: String, call: ApplicationCall)

    suspend fun waitForLogin(correlationId: String): LoginProviderResult
}
