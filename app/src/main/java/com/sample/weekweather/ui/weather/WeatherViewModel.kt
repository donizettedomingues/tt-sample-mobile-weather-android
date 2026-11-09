package com.sample.weekweather.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sample.weekweather.domain.model.RefreshError
import com.sample.weekweather.domain.model.RefreshResult
import com.sample.weekweather.domain.model.WeeklyForecast
import com.sample.weekweather.domain.usecase.ObserveWeeklyForecastUseCase
import com.sample.weekweather.domain.usecase.RefreshWeeklyForecastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Behavioral pattern — Observer.
 *
 * The screen observes [uiState] (a StateFlow), which itself observes the Room-backed
 * forecast stream and the refresh status. Nobody "pushes" data into the UI: a refresh
 * writes to the cache, Room notifies its observers, and the new state flows down.
 */
@HiltViewModel
class WeatherViewModel @Inject constructor(
    observeWeeklyForecast: ObserveWeeklyForecastUseCase,
    private val refreshWeeklyForecast: RefreshWeeklyForecastUseCase,
) : ViewModel() {

    private data class RefreshStatus(
        val hasAttempted: Boolean = false,
        val isRefreshing: Boolean = false,
        val lastError: RefreshError? = null,
        val pendingMessage: RefreshError? = null,
    )

    private val refreshStatus = MutableStateFlow(RefreshStatus())
    private var started = false

    val uiState: StateFlow<WeatherUiState> =
        combine(observeWeeklyForecast(), refreshStatus, ::reduce)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = WeatherUiState(),
            )

    /**
     * Called once the location permission prompt has been answered (granted or not).
     * Idempotent so a configuration change does not trigger a second fetch.
     */
    fun onStart() {
        if (started) return
        started = true
        refresh(force = false)
    }

    /** Manual refresh from the toolbar or the empty-state retry button. */
    fun onRefresh() = refresh(force = true)

    fun onTransientErrorShown() {
        refreshStatus.update { it.copy(pendingMessage = null) }
    }

    private fun refresh(force: Boolean) {
        if (refreshStatus.value.isRefreshing) return
        refreshStatus.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val result = refreshWeeklyForecast(force)
            refreshStatus.update { status ->
                when (result) {
                    is RefreshResult.Failed -> status.copy(
                        hasAttempted = true,
                        isRefreshing = false,
                        lastError = result.error,
                        // Only a snackbar when there is cached data to keep showing;
                        // otherwise the error is the screen body (Content.Empty).
                        pendingMessage = if (result.hasCachedData) result.error else null,
                    )
                    RefreshResult.Updated, RefreshResult.SkippedFresh -> status.copy(
                        hasAttempted = true,
                        isRefreshing = false,
                        lastError = null,
                    )
                }
            }
        }
    }

    private fun reduce(forecast: WeeklyForecast?, status: RefreshStatus): WeatherUiState {
        val content = when {
            forecast != null && forecast.days.isNotEmpty() -> WeatherUiState.Content.Data(forecast)
            !status.hasAttempted || status.isRefreshing -> WeatherUiState.Content.Loading
            else -> WeatherUiState.Content.Empty(status.lastError)
        }
        return WeatherUiState(
            content = content,
            isRefreshing = status.isRefreshing,
            transientError = status.pendingMessage.takeIf { content is WeatherUiState.Content.Data },
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
