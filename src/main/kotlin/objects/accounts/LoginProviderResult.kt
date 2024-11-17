package objects.accounts

interface LoginProviderResult {
    fun isSuccessful(): Boolean
}

class LoginSuccess(val userId: String): LoginProviderResult {
    override fun isSuccessful(): Boolean {
        return true
    }
}

class LoginFailure(val reason: String): LoginProviderResult {
    override fun isSuccessful(): Boolean {
        return false
    }
}
