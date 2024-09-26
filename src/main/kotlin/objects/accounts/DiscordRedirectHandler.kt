package objects.accounts

import com.sun.net.httpserver.*
import discordLoginManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.*

//TODO: make the page more interesting looking and host this with HTTPS on the redirect URI
class DiscordRedirectHandler : HttpHandler {
    private val page = File("./resources/redirectPage.html")

    override fun handle(exchange: HttpExchange) {
        // the actual url doesn't matter, this just allows us to use builtin parsing functions
        val url = ("http://localhost" + exchange.requestURI).toHttpUrlOrNull()!!
        val state = url.queryParameter("state")
        val code = url.queryParameter("code")

        val response = discordLoginManager.getAccessToken(code!!)

        discordLoginManager.addToken(state!!, response)

        exchange.sendResponseHeaders(200, page.length())
        val outputStream = exchange.responseBody
        page.inputStream().copyTo(outputStream)
        outputStream.close()
    }
}
