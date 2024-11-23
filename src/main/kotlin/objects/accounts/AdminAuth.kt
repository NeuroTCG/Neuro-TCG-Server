package objects.accounts

import objects.*

class AdminAuth(
    private val db: GameDatabase,
) {
    fun authenticateToken(token: Token): AuthKind? {
        if (db.checkAdminToken(token)) {
            return AuthKindToken(db.getAdminTokenComment(token)!!)
        }

        val linkedUser = db.getUserIdFromToken(token) ?: return null

        if (!db.userHasFlag(linkedUser, Flag("admin"))!!) {
            return null
        }

        return AuthKindUser(linkedUser)
    }

    abstract class AuthKind

    class AuthKindUser(
        val user: TcgId,
    ) : AuthKind()

    class AuthKindToken(
        val comment: String,
    ) : AuthKind()
}
