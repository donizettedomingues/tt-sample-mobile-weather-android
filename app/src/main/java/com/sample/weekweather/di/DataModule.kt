package com.sample.weekweather.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sample.weekweather.data.local.ForecastLocalDataSource
import com.sample.weekweather.data.local.RoomForecastLocalDataSource
import com.sample.weekweather.data.location.FusedLocationProvider
import com.sample.weekweather.data.policy.AlwaysFetchPolicy
import com.sample.weekweather.data.policy.CachePolicy
import com.sample.weekweather.data.remote.ForecastRemoteDataSource
import com.sample.weekweather.data.remote.OpenMeteoRemoteDataSource
import com.sample.weekweather.data.repository.OfflineFirstWeatherRepository
import com.sample.weekweather.domain.location.LocationProvider
import com.sample.weekweather.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: OfflineFirstWeatherRepository): WeatherRepository

    @Binds
    abstract fun bindLocalDataSource(impl: RoomForecastLocalDataSource): ForecastLocalDataSource

    @Binds
    abstract fun bindRemoteDataSource(impl: OpenMeteoRemoteDataSource): ForecastRemoteDataSource

    @Binds
    abstract fun bindLocationProvider(impl: FusedLocationProvider): LocationProvider

    companion object {
        @Provides
        fun provideClock(): Clock = Clock.systemUTC()

        /**
         * Strategy selection. The challenge asks for a fetch on every app open, so production
         * uses [AlwaysFetchPolicy]; swap in `MaxAgeCachePolicy(Duration.ofMinutes(30))` to
         * throttle network calls without touching the repository.
         */
        @Provides
        fun provideCachePolicy(): CachePolicy = AlwaysFetchPolicy

        @Provides
        @Singleton
        fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)
    }
}
