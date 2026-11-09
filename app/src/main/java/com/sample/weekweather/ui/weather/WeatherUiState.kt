package com.sample.weekweather.ui.weather

import com.sample.weekweather.domain.model.RefreshError
import com.sample.weekweather.domain.model.WeeklyForecast

/**
 * Everything the screen needs to render, derived in one place (the ViewModel).
 *
 * - [content] is what fills the screen body.
 * - [transientError] is a non-blocking message (snackbar) shown *on top of* cached data.
 */
data class WeatherUiState(
    val content: Content = Content.Loading,
    val isRefreshing: Boolean = false,
    val transientError: RefreshError? = null,
) {
    sealed interface Content {
        data object Loading : Content

        /** No cached forecast and the last fetch failed (or has not produced data). */
        data class Empty(val error: RefreshError?) : Content

        data class Data(val forecast: WeeklyForecast) : Content
    }
}
