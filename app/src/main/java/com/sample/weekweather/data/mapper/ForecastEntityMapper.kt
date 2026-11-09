package com.sample.weekweather.data.mapper

import com.sample.weekweather.data.local.CachedForecast
import com.sample.weekweather.data.local.DailyForecastEntity
import com.sample.weekweather.data.local.ForecastMetaEntity
import com.sample.weekweather.domain.model.Coordinates
import com.sample.weekweather.domain.model.DailyForecast
import com.sample.weekweather.domain.model.ForecastLocation
import com.sample.weekweather.domain.model.WeatherCondition
import com.sample.weekweather.domain.model.WeeklyForecast
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Structural pattern — Adapter between Room entities and domain models (both directions). */
class ForecastEntityMapper @Inject constructor() {

    fun toDomain(cached: CachedForecast): WeeklyForecast = WeeklyForecast(
        location = ForecastLocation(
            coordinates = Coordinates(cached.meta.latitude, cached.meta.longitude),
            cityName = cached.meta.cityName,
            isFallback = cached.meta.isFallback,
        ),
        days = cached.days
            .map { it.toDomain() }
            .sortedBy { it.date },
        lastUpdated = Instant.ofEpochMilli(cached.meta.fetchedAtEpochMillis),
    )

    fun toMetaEntity(forecast: WeeklyForecast): ForecastMetaEntity = ForecastMetaEntity(
        latitude = forecast.location.coordinates.latitude,
        longitude = forecast.location.coordinates.longitude,
        cityName = forecast.location.cityName,
        isFallback = forecast.location.isFallback,
        fetchedAtEpochMillis = forecast.lastUpdated.toEpochMilli(),
    )

    fun toDayEntities(forecast: WeeklyForecast): List<DailyForecastEntity> = forecast.days.map {
        DailyForecastEntity(
            date = it.date.toString(),
            condition = it.condition.name,
            maxTemperatureC = it.maxTemperatureC,
            minTemperatureC = it.minTemperatureC,
            precipitationProbabilityPercent = it.precipitationProbabilityPercent,
        )
    }

    private fun DailyForecastEntity.toDomain() = DailyForecast(
        date = LocalDate.parse(date),
        // Unknown names (e.g. an enum value renamed in a later version) degrade gracefully.
        condition = WeatherCondition.entries.firstOrNull { it.name == condition }
            ?: WeatherCondition.UNKNOWN,
        maxTemperatureC = maxTemperatureC,
        minTemperatureC = minTemperatureC,
        precipitationProbabilityPercent = precipitationProbabilityPercent,
    )
}
