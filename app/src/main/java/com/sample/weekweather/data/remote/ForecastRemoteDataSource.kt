package com.sample.weekweather.data.remote

import com.sample.weekweather.data.mapper.ForecastDtoMapper
import com.sample.weekweather.domain.model.Coordinates
import com.sample.weekweather.domain.model.DailyForecast
import javax.inject.Inject

/** Network side of the repository. Throws on failure; the repository decides what that means. */
interface ForecastRemoteDataSource {
    suspend fun fetchDailyForecast(coordinates: Coordinates): List<DailyForecast>
}

class OpenMeteoRemoteDataSource @Inject constructor(
    private val api: OpenMeteoApi,
    private val mapper: ForecastDtoMapper,
) : ForecastRemoteDataSource {

    override suspend fun fetchDailyForecast(coordinates: Coordinates): List<DailyForecast> =
        mapper.toDomain(api.dailyForecast(coordinates.latitude, coordinates.longitude))
}
