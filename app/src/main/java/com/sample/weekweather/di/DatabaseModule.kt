package com.sample.weekweather.di

import android.content.Context
import androidx.room.Room
import com.sample.weekweather.data.local.ForecastDao
import com.sample.weekweather.data.local.WeatherDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WeatherDatabase =
        Room.databaseBuilder(context, WeatherDatabase::class.java, WeatherDatabase.NAME).build()

    @Provides
    fun provideForecastDao(database: WeatherDatabase): ForecastDao = database.forecastDao()
}
