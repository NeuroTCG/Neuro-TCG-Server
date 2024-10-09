package objects.accounts

class DiscordAccount(
    val username: String,
    val discordUID: String,
    val avatarUrl: String?,
    uID: String,
) : Account(
        uID,
        AccountType.Discord,
    )
