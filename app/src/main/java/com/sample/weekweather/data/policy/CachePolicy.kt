package com.sample.weekweather.data.policy

import com.sample.weekweather.domain.model.ForecastLocation
import com.sample.weekweather.domain.model.WeeklyForecast
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/**
 * Behavioral pattern — Strategy.
 *
 * Decides whether a non-forced refresh should hit the network. The repository depends
 * only on this interface; which strategy is used is a DI decision (see DataModule).
 */
fun interface CachePolicy {
    fun shouldFetch(cached: WeeklyForecast?, requested: ForecastLocation, now: Instant): Boolean
}

/** Fetch every time we are asked to. Used in production so opening the app always refreshes. */
object AlwaysFetchPolicy : CachePolicy {
    override fun shouldFetch(cached: WeeklyForecast?, requested: ForecastLocation, now: Instant) = true
}

/**
 * Fetch only when the cache is empty, older than [maxAge], or for a different place.
 * Useful to save data/battery if refresh were triggered more often (e.g. on every resume).
 */
class MaxAgeCachePolicy(
    private val maxAge: Duration,
    /** ~0.05° is roughly 5 km: closer than that we treat as "the same place". */
    private val sameLocationToleranceDegrees: Double = 0.05,
) : CachePolicy {

    override fun shouldFetch(cached: WeeklyForecast?, requested: ForecastLocation, now: Instant): Boolean {
        if (cached == null || cached.days.isEmpty()) return true
        if (!cached.location.isNear(requested)) return true
        return Duration.between(cached.lastUpdated, now) >= maxAge
    }

    private fun ForecastLocation.isNear(other: ForecastLocation): Boolean =
        isFallback == other.isFallback &&
            abs(coordinates.latitude - other.coordinates.latitude) <= sameLocationToleranceDegrees &&
            abs(coordinates.longitude - other.coordinates.longitude) <= sameLocationToleranceDegrees
}
