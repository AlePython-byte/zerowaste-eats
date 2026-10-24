package com.zerowasteeats.app.presentation.navigation

import com.zerowasteeats.app.data.session.Session

/** Which part of the app the root shows. Each one has its own navigation and back stack. */
sealed interface AppSpace {
    /**
     * Identifies the content shown for this space. It changes with the account, so signing in
     * with another account builds the space from scratch instead of reusing the previous one.
     */
    val key: String

    /** The saved session has not been read yet: only the theme background is drawn. */
    data object Loading : AppSpace {
        override val key = "loading"
    }

    data object Auth : AppSpace {
        override val key = "auth"
    }

    data class Customer(val session: Session.Customer) : AppSpace {
        override val key = "customer:${session.userId}"
    }

    data class Merchant(val session: Session.Merchant) : AppSpace {
        override val key = "merchant:${session.userId}"
    }
}

fun appSpaceFor(session: Session): AppSpace = when (session) {
    Session.Loading -> AppSpace.Loading
    Session.SignedOut -> AppSpace.Auth
    is Session.Customer -> AppSpace.Customer(session)
    is Session.Merchant -> AppSpace.Merchant(session)
}
