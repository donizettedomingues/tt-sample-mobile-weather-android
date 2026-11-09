package com.sample.weekweather.domain.model

import java.time.Instant
import java.time.LocalDate

/** Plain domain models: no Android, Room or JSON types leak into this package. */

data class Coordinates(val latitude: Double, val longitude: Double)

/**
 * Where a forecast is for. [cityName] is only known for the default (fallback) city;
 * for a device fix we show "Current location" because Open-Meteo does not reverse-geocode.
 */
data class ForecastLocation(
    val coordinates: Coordinates,
    val cityName: String?,
    val isFallback: Boolean,
)

enum class WeatherCondition {
    CLEAR, MAINLY_CLEAR, PARTLY_CLOUDY, OVERCAST, FOG, DRIZZLE, RAIN, FREEZING_RAIN,
    SNOW, RAIN_SHOWERS, SNOW_SHOWERS, THUNDERSTORM, UNKNOWN,
}

data class DailyForecast(
    val date: LocalDate,
    val condition: WeatherCondition,
    val maxTemperatureC: Double,
    val minTemperatureC: Double,
    /** Max chance of precipitation that day, 0–100; null when the provider has no value. */
    val precipitationProbabilityPercent: Int? = null,
)

data class WeeklyForecast(
    val location: ForecastLocation,
    val days: List<DailyForecast>,
    /** When the data was fetched from the network (not when it was read from cache). */
    val lastUpdated: Instant,
)
