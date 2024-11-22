package objects.accounts

import io.ktor.server.application.*
import io.ktor.server.routing.*

interface LoginProvider {
    val name: String

    fun registerAdditionalRoutes(route: Route) { }

    // This should handle the initial provider request, and internally acknowledge the correlation id
    suspend fun handleInitialRequest(
        correlationId: CorrelationId,
        call: ApplicationCall,
    )

    suspend fun waitForLogin(correlationId: CorrelationId): LoginProviderResult?
}
