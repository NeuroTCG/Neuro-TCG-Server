package objects.accounts

import java.util.*

class AccountStore {
    private val accounts = mutableMapOf<String, Account>()


    fun getAccount(userId: String): Account? {
        return accounts[userId]
    }

    fun removeAccount(userId: String) {
        accounts.remove(userId)
    }
    
    private fun addAccount(account: Account) {
        accounts[account.userId] = account
    }

    fun createAccount(): Account {
        val account = Account(newUserId());
        addAccount(account)
        return account
    }

    private fun newUserId(): String {
        return UUID.randomUUID().toString()
    }
}
