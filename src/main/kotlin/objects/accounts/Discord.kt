package objects.accounts

import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.util.concurrent.*

class Discord(
    private val discordAppToken: String,
    private val clientId: String,
    private val redirectUri: String
) {
    private val discordUrl = "https://discord.com/api/v10"
    private val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.DAYS)
        .writeTimeout(1, TimeUnit.DAYS)
        .readTimeout(1, TimeUnit.DAYS)
        .callTimeout(1, TimeUnit.DAYS)
        .build()

    fun getAccessToken(authCode: String): String {
        val discordTokenUrl = "$discordUrl/oauth2/token"

        val jsonBody = buildJsonObject {
            put("grant_type", "authorization_code")
            put("code", authCode)
            put("redirect_uri", redirectUri)
            put("client_id", clientId)
            put("client_secret", discordAppToken)
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

        val jsonBody = buildJsonObject {
            put("token_type_hint", "access_token")
            put("token", token)
            put("client_id", clientId)
            put("client_secret", discordAppToken)
        }

        var requestBody = ""
        for (i in jsonBody) {
            requestBody += "${i.key}=${i.value.jsonPrimitive.content}&"
        }
        requestBody = requestBody.dropLast(1)

        val contentType = "application/x-www-form-urlencoded"

        return sendRequest(tokenRevokeUrl, requestBody, contentType)
    }

    fun getUserData(token: String): String {
        val userDataUrl = "$discordUrl/users/@me"

        val request = Request.Builder()
            .url(userDataUrl)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("Failed to get user data. Code: ${response.code}")
                return ""
            }
            val responseJson = response.body!!.string()
            return responseJson
        }
    }

    private fun sendRequest(url: String, data: String, contentType: String): String {
        val request = Request.Builder()
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
}
