package com.zerowasteeats.app.data.merchant

/** Why the business's data could not be read or written. */
enum class MerchantDataError {
    NETWORK,

    /** The server no longer accepts the session: signing in again fixes it. */
    SESSION,
    UNEXPECTED
}

/** Where loading a repository's data stands. */
sealed interface LoadState {
    /** Nothing asked yet. */
    data object Idle : LoadState
    data object Loading : LoadState
    data object Loaded : LoadState

    /** The last load failed; whatever was loaded before is still there. */
    data class Failed(val error: MerchantDataError) : LoadState
}

/** A write that did not reach the server, or that the server refused unexpectedly. */
class MerchantDataException(val error: MerchantDataError) : Exception(error.name)
