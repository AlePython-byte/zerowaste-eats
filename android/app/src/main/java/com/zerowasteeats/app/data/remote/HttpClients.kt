package com.zerowasteeats.app.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** How the app reads and writes JSON. Unknown fields are ignored so the backend can grow. */
internal val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private const val CONNECT_TIMEOUT_SECONDS = 15L
private const val READ_TIMEOUT_SECONDS = 30L

/** The shared HTTP client. Never logs headers or bodies: they carry tokens and personal data. */
fun createHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .build()

/** Retrofit for the backend at [baseUrl], which must end in "/" (Retrofit resolves paths against it). */
fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
    require(baseUrl.endsWith("/")) { "The API base URL must end in /" }
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
}
