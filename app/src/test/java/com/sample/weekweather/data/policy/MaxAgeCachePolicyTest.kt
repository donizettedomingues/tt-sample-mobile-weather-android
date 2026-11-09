package com.sample.weekweather.data.policy

import com.sample.weekweather.testing.TestData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class MaxAgeCachePolicyTest {

    private val policy = MaxAgeCachePolicy(maxAge = Duration.ofMinutes(30))

    @Test
    fun `fetches when nothing is cached`() {
        assertTrue(policy.shouldFetch(null, TestData.deviceLocation, TestData.now))
    }

    @Test
    fun `skips when cache is fresh and for the same place`() {
        val cached = TestData.cachedForecast(lastUpdated = TestData.now.minus(Duration.ofMinutes(10)))
        assertFalse(policy.shouldFetch(cached, TestData.deviceLocation, TestData.now))
    }

    @Test
    fun `fetches when cache is stale`() {
        val cached = TestData.cachedForecast(lastUpdated = TestData.now.minus(Duration.ofMinutes(31)))
        assertTrue(policy.shouldFetch(cached, TestData.deviceLocation, TestData.now))
    }

    @Test
    fun `fetches when the location changed even if cache is fresh`() {
        val cached = TestData.cachedForecast(location = TestData.fallbackLocation, lastUpdated = TestData.now)
        assertTrue(policy.shouldFetch(cached, TestData.deviceLocation, TestData.now))
    }
}
