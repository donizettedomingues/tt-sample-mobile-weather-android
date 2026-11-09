package com.sample.weekweather.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Creational pattern — Factory (built on Retrofit/OkHttp Builders).
 *
 * Owns every decision about how the HTTP client is assembled (timeouts, JSON leniency,
 * base URL) so DI modules and any future instrumented test against a mock server
 * create the API the same way, through one call.
 */
object OpenMeteoApiFactory {

    private val json = Json {
        ignoreUnknownKeys = true // Open-Meteo adds metadata fields we don't use.
        explicitNulls = false
    }

    fun create(
        baseUrl: String,
        client: OkHttpClient = defaultClient(),
    ): OpenMeteoApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenMeteoApi::class.java)

    fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
}
