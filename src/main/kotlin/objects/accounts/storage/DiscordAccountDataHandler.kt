package objects.accounts.storage

import kotlinx.serialization.Serializable
import objects.accounts.*
import kotlin.jvm.Throws

@Serializable
data class DiscordAccountDataHandler(
    private val accounts: MutableMap<String, Triple<String, String, String?>>,
) {
    operator fun get(uID: String): DiscordAccount? {
        val accountData = accounts[uID] ?: return null
        return DiscordAccount(accountData.first, accountData.second, accountData.third, uID)
    }

    @Throws(UserIDAlreadyUsedException::class)
    fun addAccount(account: DiscordAccount) {
        if (!this.containsKey(account.uID)) {
            accounts[account.uID] = Triple(account.username, account.discordUID, account.avatarUrl)
            return
        }
        throw UserIDAlreadyUsedException()
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
