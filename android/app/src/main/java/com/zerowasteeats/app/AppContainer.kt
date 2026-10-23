package com.zerowasteeats.app

import android.content.Context
import com.zerowasteeats.app.data.auth.AuthRepository
import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.session.PrefsSessionStore
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
}
