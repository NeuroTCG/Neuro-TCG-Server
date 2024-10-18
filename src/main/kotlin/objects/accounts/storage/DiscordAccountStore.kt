package objects.accounts.storage

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import objects.accounts.*
import java.io.*

class DiscordAccountStore(
    path: String,
) {
    private val storeRoot = File(path)
    private val storeFile = File("$path/DiscordAccountStore.json")
    private val accountDataHandler: DiscordAccountDataHandler

    init {
        if (!storeRoot.exists()) {
            storeRoot.mkdir()
        }
        if (!storeFile.exists()) {
            storeFile.createNewFile()
            storeFile.writeText(Json.encodeToString(DiscordAccountDataHandler(mutableMapOf())))
        }
        accountDataHandler = Json.decodeFromString(storeFile.readText())
    }

    operator fun get(discordUID: String): DiscordAccount? {
        val account = accountDataHandler.toDiscordUIDMap()[discordUID] ?: return null
        return DiscordAccount(account.first, discordUID, account.third, account.second)
    }

    fun addAccount(account: DiscordAccount): Boolean {
        val result = accountDataHandler.addAccount(account)
        storeFile.writeText(Json.encodeToString(accountDataHandler))
        return result
    }
}
