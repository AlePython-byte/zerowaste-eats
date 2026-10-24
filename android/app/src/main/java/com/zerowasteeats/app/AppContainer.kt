package com.zerowasteeats.app

import android.content.Context
import com.zerowasteeats.app.data.auth.AuthRepository
import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.merchant.FakeMerchantData
import com.zerowasteeats.app.data.merchant.MerchantRepositories
import com.zerowasteeats.app.data.session.PrefsSessionStore
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.data.session.SessionManager
import com.zerowasteeats.app.data.session.SharedPreferencesKeyValueStore
import com.zerowasteeats.app.data.session.SessionStore

/**
 * Builds the app's long-lived objects by hand. Swapping the fake repositories for the real
 * ones later happens here, and nothing that uses them changes.
 */
class AppContainer(context: Context) {

    val authRepository: AuthRepository = FakeAuthRepository()

    val sessionStore: SessionStore = PrefsSessionStore(
        SharedPreferencesKeyValueStore(context, PrefsSessionStore.FILE_NAME)
    )

    val sessionManager = SessionManager(authRepository, sessionStore)

    private val merchantRepositories = mutableMapOf<String, MerchantRepositories>()

    /**
     * The repositories of a signed-in business. Asking needs a [Session.Merchant], so the
     * customer space has no way to reach them. They are kept per account while the process
     * lives, so every screen of the business sees the same data.
     */
    fun merchantRepositories(session: Session.Merchant): MerchantRepositories =
        synchronized(merchantRepositories) {
            merchantRepositories.getOrPut(session.userId) {
                FakeMerchantData.repositoriesFor(session)
            }
        }
}
