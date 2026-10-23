package com.zerowasteeats.app.data.session

import android.content.Context

/** The little a [SessionStore] needs from the platform, so its logic runs without Android. */
interface KeyValueStore {
    fun getString(key: String): String?

    /** Replaces everything stored with [values] in one step. An empty map empties the store. */
    fun replaceAll(values: Map<String, String>)
}

class SharedPreferencesKeyValueStore(
    context: Context,
    fileName: String
) : KeyValueStore {

    private val preferences =
        context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun replaceAll(values: Map<String, String>) {
        val editor = preferences.edit().clear()
        values.forEach { (key, value) -> editor.putString(key, value) }
        editor.apply()
    }
}
