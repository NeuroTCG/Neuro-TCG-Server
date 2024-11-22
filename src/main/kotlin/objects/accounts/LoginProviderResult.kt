package objects.accounts

import objects.*

interface LoginProviderResult {
    val isSuccessful: Boolean
}

class LoginSuccess(
    val userId: TcgId,
) : LoginProviderResult {
    override val isSuccessful = true
}

class LoginFailure(
    val reason: String,
) : LoginProviderResult {
    override val isSuccessful = false
}

class LoginException(
    val exception: Exception,
) : LoginProviderResult {
    override val isSuccessful = false
}
