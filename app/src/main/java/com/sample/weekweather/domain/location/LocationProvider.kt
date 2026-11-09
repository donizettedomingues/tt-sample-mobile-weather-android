package com.sample.weekweather.domain.location

import com.sample.weekweather.domain.model.ForecastLocation

/**
 * Abstraction over the device location so the domain and tests never depend on
 * Google Play Services. Implementations must never throw: when no fix is available
 * (permission denied, location off, timeout) they return the default city.
 */
interface LocationProvider {
    suspend fun currentLocation(): ForecastLocation
}
