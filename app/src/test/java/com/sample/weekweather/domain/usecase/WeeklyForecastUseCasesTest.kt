package com.sample.weekweather.domain.usecase

import app.cash.turbine.test
import com.sample.weekweather.data.policy.AlwaysFetchPolicy
import com.sample.weekweather.data.repository.OfflineFirstWeatherRepository
import com.sample.weekweather.domain.model.RefreshResult
import com.sample.weekweather.testing.FakeLocalDataSource
import com.sample.weekweather.testing.FakeLocationProvider
import com.sample.weekweather.testing.FakeRemoteDataSource
import com.sample.weekweather.testing.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyForecastUseCasesTest {

    private val local = FakeLocalDataSource()
    private val remote = FakeRemoteDataSource()
    private val locationProvider = FakeLocationProvider()
    private val repository = OfflineFirstWeatherRepository(local, remote, AlwaysFetchPolicy, TestData.clock)

    private val refresh = RefreshWeeklyForecastUseCase(locationProvider, repository)
    private val observe = ObserveWeeklyForecastUseCase(repository)

    @Test
    fun `refresh fetches the forecast for the device location`() = runTest {
        val result = refresh()

        assertEquals(RefreshResult.Updated, result)
        assertEquals(listOf(TestData.deviceLocation.coordinates), remote.requestedCoordinates)
    }

    @Test
    fun `refresh uses the default city when the provider falls back`() = runTest {
        locationProvider.location = TestData.fallbackLocation

        refresh()

        assertEquals(listOf(TestData.fallbackLocation.coordinates), remote.requestedCoordinates)
        assertEquals(TestData.fallbackLocation, local.stored.value?.location)
    }

    @Test
    fun `observers are notified when a refresh writes new data`() = runTest {
        observe().test {
            assertNull(awaitItem()) // empty cache
            refresh()
            assertEquals(TestData.week, awaitItem()?.days)
        }
    }
}
