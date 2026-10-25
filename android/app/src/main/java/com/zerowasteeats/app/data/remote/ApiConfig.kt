package com.zerowasteeats.app.data.remote

import com.zerowasteeats.app.BuildConfig

/**
 * Where the app finds the backend and Supabase. The values come from local.properties at
 * build time, per build type, so no endpoint or key lives in the repository.
 */
data class ApiConfig(
    /** Backend base URL, ending in "/". */
    val apiBaseUrl: String,
    val supabaseUrl: String,
    /** Public by design; Supabase pairs it with row level security. */
    val supabasePublishableKey: String
) {
    /** False when something is missing: then only demo data can work. */
    val isComplete: Boolean
        get() = apiBaseUrl.isNotBlank() &&
            supabaseUrl.isNotBlank() &&
            supabasePublishableKey.isNotBlank()

    companion object {
        fun fromBuildConfig() = ApiConfig(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabasePublishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        )
    }
}
