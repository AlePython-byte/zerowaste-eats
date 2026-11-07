package com.zerowasteeats.app.data.settings

/** What came of building the live clients from the endpoints in the configuration. */
sealed interface LiveSetup<out T> {

    data class Ready<T>(val clients: T) : LiveSetup<T>

    /** No endpoints were given, so the demo is all there is by design. */
    data object NotConfigured : LiveSetup<Nothing>

    /**
     * The endpoints are there but the clients refused them. The build checks them first, so
     * this is a last line of defence: the app starts with demo data instead of closing.
     */
    data object Invalid : LiveSetup<Nothing>

    val clientsOrNull: T? get() = (this as? Ready)?.clients
}

/**
 * Runs [build] when [configured]. OkHttp and Retrofit report a URL they cannot use with
 * [IllegalArgumentException]; that becomes [LiveSetup.Invalid] rather than a crash at launch.
 */
fun <T> buildLiveSetup(configured: Boolean, build: () -> T): LiveSetup<T> {
    if (!configured) return LiveSetup.NotConfigured
    return try {
        LiveSetup.Ready(build())
    } catch (_: IllegalArgumentException) {
        LiveSetup.Invalid
    }
}
