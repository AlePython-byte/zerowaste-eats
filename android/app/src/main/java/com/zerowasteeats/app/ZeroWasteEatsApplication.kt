package com.zerowasteeats.app

import android.app.Application
import com.zerowasteeats.app.presentation.theme.ThemePreferences

class ZeroWasteEatsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Before any activity attaches its base context, so AppCompat resolves
        // the night resources from the saved app mode rather than the system's.
        ThemePreferences.load(this)
    }
}
