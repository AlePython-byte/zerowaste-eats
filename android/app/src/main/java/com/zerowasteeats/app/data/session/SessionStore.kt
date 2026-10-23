package com.zerowasteeats.app.data.session

/** Keeps the session between launches. Holds identity only, never a password or a token. */
interface SessionStore {
    /** The saved session: [Session.SignedOut] if none, never [Session.Loading]. */
    fun load(): Session

    /** Saves [session]. [Session.Loading] and [Session.SignedOut] erase what was saved. */
    fun save(session: Session)

    fun clear()
}

class PrefsSessionStore(private val store: KeyValueStore) : SessionStore {

    override fun load(): Session {
        val userId = store.getString(KEY_USER_ID)
        val displayName = store.getString(KEY_DISPLAY_NAME)
        val email = store.getString(KEY_EMAIL)
        val role = store.getString(KEY_ROLE)?.let { name ->
            UserRole.entries.firstOrNull { it.name == name }
        }
        if (userId.isNullOrEmpty() || displayName == null || email == null || role == null) {
            return discardUnreadable()
        }

        return when (role) {
            UserRole.CUSTOMER -> Session.Customer(userId, displayName, email)
            UserRole.MERCHANT -> {
                val businessName = store.getString(KEY_BUSINESS_NAME) ?: return discardUnreadable()
                Session.Merchant(userId, displayName, email, businessName)
            }
        }
    }

    override fun save(session: Session) {
        when (session) {
            is Session.Customer -> store.replaceAll(
                mapOf(
                    KEY_USER_ID to session.userId,
                    KEY_ROLE to UserRole.CUSTOMER.name,
                    KEY_DISPLAY_NAME to session.displayName,
                    KEY_EMAIL to session.email
                )
            )
            is Session.Merchant -> store.replaceAll(
                mapOf(
                    KEY_USER_ID to session.userId,
                    KEY_ROLE to UserRole.MERCHANT.name,
                    KEY_DISPLAY_NAME to session.displayName,
                    KEY_EMAIL to session.email,
                    KEY_BUSINESS_NAME to session.businessName
                )
            )
            Session.Loading, Session.SignedOut -> clear()
        }
    }

    override fun clear() = store.replaceAll(emptyMap())

    // Half-written or tampered data must not leave the person stuck in a broken session
    private fun discardUnreadable(): Session {
        if (store.getString(KEY_USER_ID) != null || store.getString(KEY_ROLE) != null) clear()
        return Session.SignedOut
    }

    companion object {
        /** SharedPreferences file name. The backup rules exclude this same file. */
        const val FILE_NAME = "session_prefs"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_BUSINESS_NAME = "business_name"
    }
}
