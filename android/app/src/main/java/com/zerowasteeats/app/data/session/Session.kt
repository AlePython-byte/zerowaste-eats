package com.zerowasteeats.app.data.session

/** Who is using the app, and so which space they see. */
sealed interface Session {
    /** The saved session has not been read yet. */
    data object Loading : Session

    data object SignedOut : Session

    data class Customer(
        val userId: String,
        val displayName: String,
        val email: String
    ) : Session

    data class Merchant(
        val userId: String,
        val displayName: String,
        val email: String,
        val businessName: String
    ) : Session
}
