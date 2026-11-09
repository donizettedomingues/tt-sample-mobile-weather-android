package com.sample.weekweather.data.location

import com.sample.weekweather.domain.model.Coordinates
import com.sample.weekweather.domain.model.ForecastLocation

/**
 * Fallback used when the location permission is denied or no fix is available.
 * Montréal, QC — documented in the README ("Offline & location behaviour").
 */
object DefaultCity {
    const val NAME = "Montréal"
    val location = ForecastLocation(
        coordinates = Coordinates(latitude = 45.5019, longitude = -73.5674),
        cityName = NAME,
        isFallback = true,
    )
}
