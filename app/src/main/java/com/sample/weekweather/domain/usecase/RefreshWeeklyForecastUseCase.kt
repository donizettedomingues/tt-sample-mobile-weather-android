package com.sample.weekweather.domain.usecase

import com.sample.weekweather.domain.location.LocationProvider
import com.sample.weekweather.domain.model.RefreshResult
import com.sample.weekweather.domain.repository.WeatherRepository
import javax.inject.Inject

/**
 * Resolves where the user is (or the default city) and asks the repository to refresh
 * the forecast for that place. [force] bypasses the cache policy (manual refresh).
 */
class RefreshWeeklyForecastUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val repository: WeatherRepository,
) {
    suspend operator fun invoke(force: Boolean = false): RefreshResult {
        val location = locationProvider.currentLocation()
        return repository.refresh(location, force)
    }
}
