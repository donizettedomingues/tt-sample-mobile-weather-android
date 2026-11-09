package com.sample.weekweather.data.mapper

import com.sample.weekweather.data.remote.dto.ForecastResponseDto
import com.sample.weekweather.domain.model.DailyForecast
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * Structural pattern — Adapter (a.k.a. Mapper).
 *
 * Adapts Open-Meteo's column-oriented response (parallel arrays) into the row-oriented
 * domain list the rest of the app understands. Days with a missing date or temperature
 * are dropped instead of being shown as "0°".
 */
class ForecastDtoMapper @Inject constructor(
    private val weatherCodeMapper: WeatherCodeMapper,
) {

    fun toDomain(dto: ForecastResponseDto): List<DailyForecast> {
        val daily = dto.daily ?: return emptyList()
        return daily.time.mapIndexedNotNull { index, rawDate ->
            val date = rawDate.toLocalDateOrNull() ?: return@mapIndexedNotNull null
            val max = daily.temperatureMax.getOrNull(index) ?: return@mapIndexedNotNull null
            val min = daily.temperatureMin.getOrNull(index) ?: return@mapIndexedNotNull null
            DailyForecast(
                date = date,
                condition = weatherCodeMapper.map(daily.weatherCode.getOrNull(index)),
                maxTemperatureC = max,
                minTemperatureC = min,
                precipitationProbabilityPercent = daily.precipitationProbabilityMax.getOrNull(index),
            )
        }
    }

    private fun String.toLocalDateOrNull(): LocalDate? =
        try {
            LocalDate.parse(this)
        } catch (e: DateTimeParseException) {
            null
        }
}
