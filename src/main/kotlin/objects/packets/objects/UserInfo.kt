package objects.packets.objects

import discordLoginManager
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.IOException

@Serializable
class UserInfo(
    @Required val username: String,
    @Required val region: String,
    val avatarUrl: String = "",
) {
    companion object {
        fun fromOauth(oAuthData: String): Pair<UserInfo?, Pair<String, String>?>? {
            try {
                val jsonData = Json.decodeFromString<JsonObject>(oAuthData)
                return when (jsonData["oauth_type"]?.jsonPrimitive?.content) {
                    null -> {
                        println("Warning: invalid OAuth data received")
                        null
                    }
                    "discord" -> {
                        var newTokens: Pair<String, String>? = null
                        if (jsonData["has_session"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() == true) {
                            val user =
                                try {
                                    jsonData["token"]?.jsonPrimitive?.content?.let { discordLoginManager.getUserData(it) }
                                } catch (e: IOException) {
                                    newTokens =
                                        jsonData["refresh_token"]?.jsonPrimitive?.content?.let {
                                            discordLoginManager.refreshToken(
                                                it,
                                            )
                                        }
                                    if (newTokens == null) {
                                        println("Warning: token refresh failed")
                                        null
                                    } else {
                                        discordLoginManager.getUserData(newTokens.first)
                                    }
                                }
                            if (user == null) {
                                null
                            } else {
                                Pair(UserInfo(user.username, "Who knows", user.avatarUrl ?: ""), null)
                            }
                        } else {
                            val tokens =
                                discordLoginManager
                                    .getAccessTokenFromState(jsonData["state"]!!.jsonPrimitive.content)
                            val user = tokens?.first?.let { discordLoginManager.getUserData(it) }

                            if (user == null) {
                                null
                            } else {
                                Pair(UserInfo(user.username, "Who knows", user.avatarUrl ?: ""), tokens)
                            }
                        }
                    }
                    else -> {
                        println("Found unrecognized oauth type '${jsonData["oauth_type"]?.jsonPrimitive?.content}'")
                        null
                    }
                }
            } catch (e: SerializationException) {
                println("Warning: invalid OAuth data received")
                return null
            }
        }
    }
}
