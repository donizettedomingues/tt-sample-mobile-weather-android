package com.sample.weekweather.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shape of `GET /v1/forecast?daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max`.
 * Open-Meteo returns the daily values as parallel arrays indexed by day; individual
 * values may be null, so every element is nullable and the mapper decides what to drop.
 */
@Serializable
data class ForecastResponseDto(
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    val daily: DailyDto? = null,
)

@Serializable
data class DailyDto(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
)
