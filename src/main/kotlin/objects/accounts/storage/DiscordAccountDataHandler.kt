package objects.accounts.storage

import kotlinx.serialization.Serializable
import objects.accounts.*

@Serializable
data class DiscordAccountDataHandler(
    private val accounts: MutableMap<String, Triple<String, String, String?>>,
) {
    operator fun get(uID: String): DiscordAccount? {
        val accountData = accounts[uID] ?: return null
        return DiscordAccount(accountData.first, accountData.second, accountData.third, uID)
    }

    fun addAccount(account: DiscordAccount): Boolean {
        if (this.containsKey(account.uID)) {
            return false
        }
        accounts[account.uID] = Triple(account.username, account.discordUID, account.avatarUrl)
        return true
    }

    fun toDiscordUIDMap(): Map<String, Triple<String, String, String?>> =
        buildMap {
            accounts.forEach {
                put(it.value.second, Triple(it.value.first, it.key, it.value.third))
            }
        }

    fun contains(account: DiscordAccount): Boolean = this[account.uID] == account

    private fun containsKey(uID: String): Boolean = accounts.containsKey(uID)
}
