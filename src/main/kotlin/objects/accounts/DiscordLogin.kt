package objects.accounts

import kotlinx.serialization.json.*
import objects.accounts.storage.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.time.*
import java.util.concurrent.*
import kotlin.Throws
import kotlin.run

// TODO: make a function or something to integrate this with the main socket
class DiscordLogin(
    private val clientSecret: String,
    private val clientId: String,
    private val redirectUri: String,
) {
    private val discordUrl = "https://discord.com/api/v10"
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(1, TimeUnit.DAYS)
            .writeTimeout(1, TimeUnit.DAYS)
            .readTimeout(1, TimeUnit.DAYS)
            .callTimeout(1, TimeUnit.DAYS)
            .build()
    private val allTokens = mutableMapOf<String, Pair<String, Instant>>()
    private val accountStore = DiscordAccountStore("./DiscordAccountDatabase")
    private val maxAttemptUIDRetry = 5

    fun getAccessToken(authCode: String): String {
        val discordTokenUrl = "$discordUrl/oauth2/token"

        val jsonBody =
            buildJsonObject {
                put("grant_type", "authorization_code")
                put("code", authCode)
                put("redirect_uri", redirectUri)
                put("client_id", clientId)
                put("client_secret", clientSecret)
            }

        var requestBody = ""
        for (i in jsonBody) {
            requestBody += "${i.key}=${i.value.jsonPrimitive.content}&"
        }
        requestBody = requestBody.dropLast(1)
        val contentType = "application/x-www-form-urlencoded"

        return sendRequest(discordTokenUrl, requestBody, contentType)
    }

    fun revokeToken(token: String): String {
        val tokenRevokeUrl = "$discordUrl/oauth2/token/revoke"

        val jsonBody =
            buildJsonObject {
                put("token_type_hint", "access_token")
                put("token", token)
                put("client_id", clientId)
                put("client_secret", clientSecret)
            }

        var requestBody = ""
        for (i in jsonBody) {
            requestBody += "${i.key}=${i.value.jsonPrimitive.content}&"
        }
        requestBody = requestBody.dropLast(1)

        val contentType = "application/x-www-form-urlencoded"

        return sendRequest(tokenRevokeUrl, requestBody, contentType)
    }

    @Throws(IOException::class)
    fun getUserData(token: String): DiscordAccount? {
        val userDataUrl = "$discordUrl/users/@me"

        val request =
            Request
                .Builder()
                .url(userDataUrl)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("Failed to get user data. Code: ${response.code}")
                throw IOException("Failed to get user data. Code: ${response.code}")
            }
            val responseJson = Json.decodeFromString<JsonObject>(response.body!!.string())
            val username = responseJson["username"]!!.jsonPrimitive.content
            val avatarId = responseJson["avatar"]?.jsonPrimitive?.content
            val userId = responseJson["id"]?.jsonPrimitive?.content
            val avatarUrl =
                if (avatarId != null && userId != null) {
                    "https://cdn.discordapp.com/avatars/$userId/$avatarId"
                } else {
                    null
                }
            return getUser(userId!!, username, avatarUrl)
        }
    }

    private fun getUser(
        discordUID: String,
        username: String,
        avatarUrl: String?,
    ): DiscordAccount? {
        return accountStore[discordUID] ?: run {
            var newAccount: DiscordAccount
            var attempts = 0
            while (true) {
                if (attempts >= maxAttemptUIDRetry) {
                    println("Error: failed to generate user ID, user ID generation retry maximum reached!")
                    return null
                }
                newAccount = DiscordAccount(username, discordUID, avatarUrl, generateUID())
                attempts++
                if (accountStore.addAccount(newAccount)) {
                    break
                } else {
                    println("Warning: encountered already used user ID")
                    continue
                }
            }
            return newAccount
        }
    }

    private fun generateUID(): String =
        (1..18)
            .map { ('0'..'9').random() }
            .joinToString("")

    private fun sendRequest(
        url: String,
        data: String,
        contentType: String,
    ): String {
        val request =
            Request
                .Builder()
                .url(url)
                .header("Content-Type", contentType)
                .post(data.toRequestBody())
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val responseJson = response.body!!.string()
            return responseJson
        }
    }

    fun addToken(
        state: String,
        response: String,
    ) {
        allTokens[state.take(16)] = response to (Instant.now().plusSeconds(3 * 60))
        for (i in allTokens) {
            if (i.value.second.isBefore(Instant.now())) {
                allTokens.remove(i.key)
            }
        }
    }

    fun getAccessTokenFromState(state: String): String? {
        // returns null if the state is invalid, someone can handle that
        if (!allTokens.containsKey(state)) {
            return null
        }

        val response = allTokens[state]!!.first
        allTokens.remove(state)

        return response
    }
}
