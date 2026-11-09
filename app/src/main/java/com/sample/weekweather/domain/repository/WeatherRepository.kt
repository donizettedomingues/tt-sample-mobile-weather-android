package com.sample.weekweather.domain.repository

import com.sample.weekweather.domain.model.ForecastLocation
import com.sample.weekweather.domain.model.RefreshResult
import com.sample.weekweather.domain.model.WeeklyForecast
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first source of truth for the weekly forecast.
 *
 * Reads always come from the local cache ([observeForecast]); the network only ever
 * writes into the cache ([refresh]). The UI therefore never has to choose between
 * "network data" and "cached data".
 */
interface WeatherRepository {
    fun observeForecast(): Flow<WeeklyForecast?>

    suspend fun refresh(location: ForecastLocation, force: Boolean): RefreshResult
}
