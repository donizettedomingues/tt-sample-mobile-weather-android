package com.sample.weekweather.domain.usecase

import com.sample.weekweather.domain.model.WeeklyForecast
import com.sample.weekweather.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the cached forecast; emits again every time a refresh writes new data. */
class ObserveWeeklyForecastUseCase @Inject constructor(
    private val repository: WeatherRepository,
) {
    operator fun invoke(): Flow<WeeklyForecast?> = repository.observeForecast()
}
