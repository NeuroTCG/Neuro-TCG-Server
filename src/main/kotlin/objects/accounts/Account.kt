package objects.accounts

open class Account(
    val uID: String,
    val accountType: AccountType = AccountType.None
)

