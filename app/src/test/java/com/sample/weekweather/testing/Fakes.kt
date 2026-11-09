package com.sample.weekweather.testing

import com.sample.weekweather.data.local.ForecastLocalDataSource
import com.sample.weekweather.data.remote.ForecastRemoteDataSource
import com.sample.weekweather.domain.location.LocationProvider
import com.sample.weekweather.domain.model.Coordinates
import com.sample.weekweather.domain.model.DailyForecast
import com.sample.weekweather.domain.model.ForecastLocation
import com.sample.weekweather.domain.model.WeatherCondition
import com.sample.weekweather.domain.model.WeeklyForecast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** In-memory stand-in for Room: same "observe + replace" contract. */
class FakeLocalDataSource(initial: WeeklyForecast? = null) : ForecastLocalDataSource {
    val stored = MutableStateFlow(initial)
    var saveCount = 0
        private set

    override fun observeForecast(): Flow<WeeklyForecast?> = stored

    override suspend fun saveForecast(forecast: WeeklyForecast) {
        saveCount++
        stored.value = forecast
    }
}

/** Programmable network: returns [days] or throws [failure]; records every call. */
class FakeRemoteDataSource(
    var days: List<DailyForecast> = TestData.week,
    var failure: Exception? = null,
) : ForecastRemoteDataSource {
    val requestedCoordinates = mutableListOf<Coordinates>()

    override suspend fun fetchDailyForecast(coordinates: Coordinates): List<DailyForecast> {
        requestedCoordinates += coordinates
        failure?.let { throw it }
        return days
    }

    fun goOffline() {
        failure = IOException("offline")
    }
}

class FakeLocationProvider(var location: ForecastLocation = TestData.deviceLocation) : LocationProvider {
    override suspend fun currentLocation(): ForecastLocation = location
}

object TestData {
    val now: Instant = Instant.parse("2026-11-09T12:00:00Z")
    val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    val deviceLocation = ForecastLocation(Coordinates(43.65, -79.38), cityName = null, isFallback = false)
    val fallbackLocation = ForecastLocation(Coordinates(45.5019, -73.5674), cityName = "Montréal", isFallback = true)

    val week: List<DailyForecast> = (0L until 7L).map { offset ->
        DailyForecast(
            date = LocalDate.of(2026, 11, 9).plusDays(offset),
            condition = WeatherCondition.PARTLY_CLOUDY,
            maxTemperatureC = 10.0 + offset,
            minTemperatureC = 1.0 + offset,
            precipitationProbabilityPercent = (offset * 10).toInt(),
        )
    }

    fun cachedForecast(
        location: ForecastLocation = deviceLocation,
        lastUpdated: Instant = now.minusSeconds(3_600),
    ) = WeeklyForecast(location, week, lastUpdated)
}
