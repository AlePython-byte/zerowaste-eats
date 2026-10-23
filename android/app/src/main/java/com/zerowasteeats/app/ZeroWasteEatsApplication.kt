package com.zerowasteeats.app

import android.app.Application
import com.zerowasteeats.app.presentation.theme.ThemePreferences

class ZeroWasteEatsApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Before any activity attaches its base context, so AppCompat resolves
        // the night resources from the saved app mode rather than the system's.
        ThemePreferences.load(this)

        container = AppContainer(this)
        // Synchronous, so the first frame already knows who is signed in
        container.sessionManager.restore()
    }
}
