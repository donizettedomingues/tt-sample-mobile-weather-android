package com.sample.weekweather.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sample.weekweather.domain.location.LocationProvider
import com.sample.weekweather.domain.model.Coordinates
import com.sample.weekweather.domain.model.ForecastLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * [LocationProvider] backed by Google's FusedLocationProviderClient.
 *
 * Coarse (city-level) accuracy is enough for a daily forecast and is what we ask for
 * first. Any failure — permission denied, location services off, no fix within
 * [FIX_TIMEOUT_MS] — falls back to [DefaultCity] instead of failing the refresh.
 */
class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationProvider {

    override suspend fun currentLocation(): ForecastLocation {
        if (!hasLocationPermission()) return DefaultCity.location
        val fix = try {
            currentFix() ?: lastKnownFix()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // SecurityException if permission was revoked mid-call, ApiException, ...
        }
        return fix?.let {
            ForecastLocation(Coordinates(it.latitude, it.longitude), cityName = null, isFallback = false)
        } ?: DefaultCity.location
    }

    @SuppressLint("MissingPermission") // Checked in currentLocation().
    private suspend fun currentFix(): Location? {
        val cancellation = CancellationTokenSource()
        return try {
            withTimeoutOrNull(FIX_TIMEOUT_MS) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token).await()
            }
        } finally {
            cancellation.cancel()
        }
    }

    @SuppressLint("MissingPermission") // Checked in currentLocation().
    private suspend fun lastKnownFix(): Location? = client.lastLocation.await()

    private fun hasLocationPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    private companion object {
        const val FIX_TIMEOUT_MS = 5_000L
    }
}
