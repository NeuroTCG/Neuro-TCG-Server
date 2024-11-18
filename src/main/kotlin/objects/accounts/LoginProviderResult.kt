package objects.accounts

interface LoginProviderResult {
    val isSuccessful: Boolean
}

class LoginSuccess(
    val userId: String,
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
