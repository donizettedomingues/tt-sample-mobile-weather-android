package com.sample.weekweather.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ForecastMetaEntity::class, DailyForecastEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun forecastDao(): ForecastDao

    companion object {
        const val NAME = "week-weather.db"
    }
}
