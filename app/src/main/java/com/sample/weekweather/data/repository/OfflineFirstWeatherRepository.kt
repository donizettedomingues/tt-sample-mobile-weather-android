package com.sample.weekweather.data.repository

import com.sample.weekweather.data.local.ForecastLocalDataSource
import com.sample.weekweather.data.policy.CachePolicy
import com.sample.weekweather.data.remote.ForecastRemoteDataSource
import com.sample.weekweather.domain.model.ForecastLocation
import com.sample.weekweather.domain.model.RefreshError
import com.sample.weekweather.domain.model.RefreshResult
import com.sample.weekweather.domain.model.WeeklyForecast
import com.sample.weekweather.domain.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import javax.inject.Inject

/**
 * Structural pattern — Facade (Repository).
 *
 * Hides the remote API, the Room cache and the cache policy behind the two calls of
 * [WeatherRepository]. Offline-first rules:
 *  - the cache is the single source of truth: the UI only observes [observeForecast];
 *  - a successful fetch replaces the cache (which then re-emits);
 *  - a failed fetch never touches the cache, so the last good forecast stays visible.
 */
class OfflineFirstWeatherRepository @Inject constructor(
    private val local: ForecastLocalDataSource,
    private val remote: ForecastRemoteDataSource,
    private val cachePolicy: CachePolicy,
    private val clock: Clock,
) : WeatherRepository {

    override fun observeForecast(): Flow<WeeklyForecast?> = local.observeForecast()

    override suspend fun refresh(location: ForecastLocation, force: Boolean): RefreshResult {
        val cached = local.observeForecast().first()
        if (!force && !cachePolicy.shouldFetch(cached, location, clock.instant())) {
            return RefreshResult.SkippedFresh
        }
        return try {
            val days = remote.fetchDailyForecast(location.coordinates)
            if (days.isEmpty()) {
                // A 200 with no usable days must not wipe a good cache.
                RefreshResult.Failed(RefreshError.SERVER, hasCachedData = cached != null)
            } else {
                local.saveForecast(WeeklyForecast(location, days, lastUpdated = clock.instant()))
                RefreshResult.Updated
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RefreshResult.Failed(e.toRefreshError(), hasCachedData = cached != null)
        }
    }

    private fun Exception.toRefreshError(): RefreshError = when (this) {
        is IOException -> RefreshError.NO_CONNECTION
        is HttpException, is SerializationException -> RefreshError.SERVER
        else -> RefreshError.UNKNOWN
    }
}
