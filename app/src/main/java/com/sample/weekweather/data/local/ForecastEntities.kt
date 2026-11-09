package com.sample.weekweather.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * The app caches exactly one forecast (the last successful one), so the meta table
 * holds a single row with id = [ForecastMetaEntity.SINGLETON_ID].
 */
@Entity(tableName = "forecast_meta")
data class ForecastMetaEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val latitude: Double,
    val longitude: Double,
    val cityName: String?,
    val isFallback: Boolean,
    val fetchedAtEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

@Entity(
    tableName = "daily_forecast",
    primaryKeys = ["forecastId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = ForecastMetaEntity::class,
            parentColumns = ["id"],
            childColumns = ["forecastId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DailyForecastEntity(
    val forecastId: Int = ForecastMetaEntity.SINGLETON_ID,
    /** ISO-8601 date (yyyy-MM-dd) in the forecast location's time zone. */
    val date: String,
    /** [com.sample.weekweather.domain.model.WeatherCondition] name. */
    val condition: String,
    val maxTemperatureC: Double,
    val minTemperatureC: Double,
    val precipitationProbabilityPercent: Int?,
)

data class CachedForecast(
    @Embedded val meta: ForecastMetaEntity,
    @Relation(parentColumn = "id", entityColumn = "forecastId")
    val days: List<DailyForecastEntity>,
)
