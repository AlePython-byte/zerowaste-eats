package com.zerowasteeats.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET

/** The backend's public health check: answers once the server is up (and awake, when it sleeps). */
interface HealthApi {
    @GET("salud")
    suspend fun health(): HealthResponse
}

@Serializable
data class HealthResponse(
    /** "ok" when the API is up. */
    val estado: String,
    val servicio: String? = null,
    val version: String? = null
)
