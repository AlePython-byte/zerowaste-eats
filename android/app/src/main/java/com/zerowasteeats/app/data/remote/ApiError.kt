package com.zerowasteeats.app.data.remote

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** An error body as the backend sends it: `{ message, error, statusCode }`. */
@Serializable
internal data class ApiErrorBody(
    /** Text, or a list of texts when several validations fail. */
    val message: JsonElement? = null,
    val error: String? = null,
    val statusCode: Int? = null
)

/** The backend's error, already in Spanish and safe to show. */
data class ApiError(val statusCode: Int?, val message: String?)

/** Reads an error body; null when it is not one (an HTML error page from a proxy, say). */
fun parseApiError(body: String): ApiError? {
    val parsed = try {
        ApiJson.decodeFromString<ApiErrorBody>(body)
    } catch (_: SerializationException) {
        return null
    } catch (_: IllegalArgumentException) {
        return null
    }
    val message = when (val raw = parsed.message) {
        is JsonPrimitive -> raw.contentOrNull
        is JsonArray -> (raw.firstOrNull() as? JsonPrimitive)?.contentOrNull
        else -> null
    }
    return ApiError(statusCode = parsed.statusCode, message = message)
}
