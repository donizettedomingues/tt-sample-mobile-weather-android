package com.sample.weekweather.domain.model

/** Why a refresh failed, in terms the UI can turn into a message. */
enum class RefreshError { NO_CONNECTION, SERVER, UNKNOWN }

sealed interface RefreshResult {
    data object Updated : RefreshResult

    /** The cache policy decided the cached data is fresh enough; no network call was made. */
    data object SkippedFresh : RefreshResult

    data class Failed(val error: RefreshError, val hasCachedData: Boolean) : RefreshResult
}
