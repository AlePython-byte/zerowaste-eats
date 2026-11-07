package com.zerowasteeats.app.data.auth.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Supabase Auth's REST API, relative to `<project>/auth/v1/`. Every call also carries the
 * publishable key in the `apikey` header (added by the client, see [SupabaseAuthClient.create]).
 */
interface SupabaseAuthApi {
    @POST("signup")
    suspend fun signUp(@Body body: SignUpBody): SessionResponse

    @POST("token?grant_type=password")
    suspend fun signInWithPassword(@Body body: PasswordGrantBody): SessionResponse

    @POST("token?grant_type=refresh_token")
    suspend fun refresh(@Body body: RefreshGrantBody): SessionResponse

    @POST("logout")
    suspend fun signOut(@Header("Authorization") authorization: String): Response<Unit>

    @GET("user")
    suspend fun user(@Header("Authorization") authorization: String): UserResponse
}

/**
 * What the app keeps in the Supabase user's metadata at sign up, so a profile the backend never
 * received can still be created on a later sign in. Every field is optional when read back.
 */
@Serializable
data class ProfileMetadata(
    @SerialName("display_name") val displayName: String? = null,
    val role: String? = null,
    @SerialName("business_name") val businessName: String? = null,
    val city: String? = null
)

@Serializable
data class SignUpBody(
    val email: String,
    val password: String,
    val data: ProfileMetadata
)

@Serializable
data class PasswordGrantBody(val email: String, val password: String)

@Serializable
data class RefreshGrantBody(@SerialName("refresh_token") val refreshToken: String)

/**
 * A session, or (for a sign up that still needs the email confirmed) only the user, with the
 * user's fields at the top level. Both shapes decode here; the token fields tell them apart.
 */
@Serializable
data class SessionResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("expires_at") val expiresAt: Long? = null,
    val user: UserResponse? = null
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: ProfileMetadata? = null
)

/** Supabase Auth's error body. Older endpoints use `error`, newer ones `error_code`. */
@Serializable
internal data class SupabaseErrorBody(
    @SerialName("error_code") val errorCode: String? = null,
    val error: String? = null
)
