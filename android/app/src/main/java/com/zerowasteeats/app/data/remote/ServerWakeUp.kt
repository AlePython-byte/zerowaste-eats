package com.zerowasteeats.app.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import okhttp3.Interceptor
import okhttp3.Response

/**
 * The deployed backend sleeps when nobody uses it, and its first answer can take most of a
 * minute while it starts. Until the backend at [apiHost] has answered once, its calls wait up
 * to [firstCallTimeoutSeconds] instead of the usual timeout, and [waitingSince] tells when the
 * oldest of them started, so the screens can say the server is waking up. Any HTTP answer
 * counts as awake, an error included: it came from a running server.
 */
class ServerWakeUp(
    private val apiHost: String,
    private val firstCallTimeoutSeconds: Int = FIRST_CALL_TIMEOUT_SECONDS,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : Interceptor {

    @Volatile
    private var awake = false

    // Start times of the calls in flight before the first answer
    private val pending = MutableStateFlow<List<Long>>(emptyList())

    private val oldest = MutableStateFlow<Long?>(null)

    /** When the oldest call still waiting for the first answer started; null once it is awake. */
    val waitingSince: StateFlow<Long?> = oldest

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (awake || !request.url.host.equals(apiHost, ignoreCase = true)) {
            return chain.proceed(request)
        }
        val startedAt = nowMillis()
        change { it + startedAt }
        try {
            val response = chain
                .withConnectTimeout(firstCallTimeoutSeconds, TimeUnit.SECONDS)
                .withReadTimeout(firstCallTimeoutSeconds, TimeUnit.SECONDS)
                .withWriteTimeout(firstCallTimeoutSeconds, TimeUnit.SECONDS)
                .proceed(request)
            awake = true
            return response
        } finally {
            if (awake) {
                pending.value = emptyList()
                oldest.value = null
            } else {
                change { it - startedAt }
            }
        }
    }

    private fun change(transform: (List<Long>) -> List<Long>) {
        pending.update(transform)
        oldest.value = pending.value.minOrNull()
    }

    companion object {
        /** A sleeping server on a free plan takes up to about a minute to answer. */
        const val FIRST_CALL_TIMEOUT_SECONDS = 60

        /** A first call slower than this is most likely waking the server: say so. */
        const val SLOW_AFTER_MILLIS = 3_000L
    }
}
