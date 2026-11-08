package com.zerowasteeats.app.data.users

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** The backend's profile of the signed-in person. The access token travels in the client. */
interface UsersApi {
    /** 404 when the account has no profile yet. */
    @GET("users/me")
    suspend fun me(): UserProfileResponse

    /** Creates the profile, or returns the existing one unchanged: safe to repeat. */
    @POST("users/me")
    suspend fun create(@Body body: CreateProfileBody): UserProfileResponse
}

@Serializable
data class UserProfileResponse(
    val id: String,
    val email: String,
    val displayName: String,
    /** "CUSTOMER" or "MERCHANT". */
    val role: String,
    val city: String? = null
)

@Serializable
data class CreateProfileBody(
    val displayName: String,
    val role: String,
    val city: String? = null,
    /** Merchants only; the backend rejects it for a customer. */
    val businessName: String? = null
)
