package com.sample.weekweather.di

import com.sample.weekweather.BuildConfig
import com.sample.weekweather.data.remote.OpenMeteoApi
import com.sample.weekweather.data.remote.OpenMeteoApiFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOpenMeteoApi(): OpenMeteoApi = OpenMeteoApiFactory.create(BuildConfig.OPEN_METEO_BASE_URL)
}
