package com.sample.weekweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ForecastDao {

    /** Room re-emits whenever either table changes — this is what drives the UI. */
    @Transaction
    @Query("SELECT * FROM forecast_meta WHERE id = ${ForecastMetaEntity.SINGLETON_ID}")
    abstract fun observeCachedForecast(): Flow<CachedForecast?>

    @Upsert
    protected abstract suspend fun upsertMeta(meta: ForecastMetaEntity)

    @Query("DELETE FROM daily_forecast")
    protected abstract suspend fun deleteAllDays()

    @Insert
    protected abstract suspend fun insertDays(days: List<DailyForecastEntity>)

    /** Atomically swaps the cached forecast so readers never see a half-written week. */
    @Transaction
    open suspend fun replace(meta: ForecastMetaEntity, days: List<DailyForecastEntity>) {
        upsertMeta(meta)
        deleteAllDays()
        insertDays(days)
    }
}
