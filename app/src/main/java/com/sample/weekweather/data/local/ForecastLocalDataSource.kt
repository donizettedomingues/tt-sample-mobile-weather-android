package com.sample.weekweather.data.local

import com.sample.weekweather.data.mapper.ForecastEntityMapper
import com.sample.weekweather.domain.model.WeeklyForecast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Persistence side of the repository, behind an interface so tests can use an in-memory fake. */
interface ForecastLocalDataSource {
    fun observeForecast(): Flow<WeeklyForecast?>

    suspend fun saveForecast(forecast: WeeklyForecast)
}

class RoomForecastLocalDataSource @Inject constructor(
    private val dao: ForecastDao,
    private val mapper: ForecastEntityMapper,
) : ForecastLocalDataSource {

    override fun observeForecast(): Flow<WeeklyForecast?> =
        dao.observeCachedForecast().map { cached -> cached?.let(mapper::toDomain) }

    override suspend fun saveForecast(forecast: WeeklyForecast) {
        dao.replace(mapper.toMetaEntity(forecast), mapper.toDayEntities(forecast))
    }
}
