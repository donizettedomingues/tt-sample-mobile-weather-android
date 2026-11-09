package com.sample.weekweather.data.repository

import com.sample.weekweather.data.policy.AlwaysFetchPolicy
import com.sample.weekweather.data.policy.CachePolicy
import com.sample.weekweather.domain.model.RefreshError
import com.sample.weekweather.domain.model.RefreshResult
import com.sample.weekweather.domain.model.WeeklyForecast
import com.sample.weekweather.testing.FakeLocalDataSource
import com.sample.weekweather.testing.FakeRemoteDataSource
import com.sample.weekweather.testing.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstWeatherRepositoryTest {

    private fun repository(
        local: FakeLocalDataSource = FakeLocalDataSource(),
        remote: FakeRemoteDataSource = FakeRemoteDataSource(),
        policy: CachePolicy = AlwaysFetchPolicy,
    ) = OfflineFirstWeatherRepository(local, remote, policy, TestData.clock)

    @Test
    fun `successful refresh persists the forecast with location and fetch time`() = runTest {
        val local = FakeLocalDataSource()
        val repo = repository(local = local)

        val result = repo.refresh(TestData.deviceLocation, force = false)

        assertEquals(RefreshResult.Updated, result)
        val stored = repo.observeForecast().first()
        assertEquals(WeeklyForecast(TestData.deviceLocation, TestData.week, TestData.now), stored)
    }

    @Test
    fun `failed refresh keeps the cached forecast and reports there is cached data`() = runTest {
        val cached = TestData.cachedForecast()
        val local = FakeLocalDataSource(initial = cached)
        val remote = FakeRemoteDataSource().apply { goOffline() }

        val result = repository(local, remote).refresh(TestData.deviceLocation, force = true)

        assertEquals(RefreshResult.Failed(RefreshError.NO_CONNECTION, hasCachedData = true), result)
        assertEquals(cached, local.stored.value)
        assertEquals(0, local.saveCount)
    }

    @Test
    fun `failed refresh with empty cache reports no cached data`() = runTest {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource().apply { goOffline() }

        val result = repository(local, remote).refresh(TestData.deviceLocation, force = false)

        assertEquals(RefreshResult.Failed(RefreshError.NO_CONNECTION, hasCachedData = false), result)
        assertNull(local.stored.value)
    }

    @Test
    fun `empty network response never overwrites a good cache`() = runTest {
        val cached = TestData.cachedForecast()
        val local = FakeLocalDataSource(initial = cached)
        val remote = FakeRemoteDataSource(days = emptyList())

        val result = repository(local, remote).refresh(TestData.deviceLocation, force = true)

        assertEquals(RefreshResult.Failed(RefreshError.SERVER, hasCachedData = true), result)
        assertEquals(cached, local.stored.value)
    }

    @Test
    fun `cache policy can skip the network unless the refresh is forced`() = runTest {
        val remote = FakeRemoteDataSource()
        val neverFetch = CachePolicy { _, _, _ -> false }
        val repo = repository(FakeLocalDataSource(TestData.cachedForecast()), remote, neverFetch)

        assertEquals(RefreshResult.SkippedFresh, repo.refresh(TestData.deviceLocation, force = false))
        assertTrue(remote.requestedCoordinates.isEmpty())

        assertEquals(RefreshResult.Updated, repo.refresh(TestData.deviceLocation, force = true))
        assertEquals(listOf(TestData.deviceLocation.coordinates), remote.requestedCoordinates)
    }
}
