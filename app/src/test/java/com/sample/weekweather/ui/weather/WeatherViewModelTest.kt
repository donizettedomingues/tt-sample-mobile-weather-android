package com.sample.weekweather.ui.weather

import com.sample.weekweather.data.policy.AlwaysFetchPolicy
import com.sample.weekweather.data.repository.OfflineFirstWeatherRepository
import com.sample.weekweather.domain.model.RefreshError
import com.sample.weekweather.domain.usecase.ObserveWeeklyForecastUseCase
import com.sample.weekweather.domain.usecase.RefreshWeeklyForecastUseCase
import com.sample.weekweather.testing.FakeLocalDataSource
import com.sample.weekweather.testing.FakeLocationProvider
import com.sample.weekweather.testing.FakeRemoteDataSource
import com.sample.weekweather.testing.MainDispatcherRule
import com.sample.weekweather.testing.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(local: FakeLocalDataSource, remote: FakeRemoteDataSource): WeatherViewModel {
        val repository = OfflineFirstWeatherRepository(local, remote, AlwaysFetchPolicy, TestData.clock)
        return WeatherViewModel(
            ObserveWeeklyForecastUseCase(repository),
            RefreshWeeklyForecastUseCase(FakeLocationProvider(), repository),
        )
    }

    /** uiState is WhileSubscribed, so keep a subscriber alive for the test's duration. */
    private fun TestScope.subscribe(vm: WeatherViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `online start shows the freshly fetched forecast`() = runTest {
        val vm = viewModel(FakeLocalDataSource(), FakeRemoteDataSource())
        subscribe(vm)

        vm.onStart()

        val state = vm.uiState.value
        val content = state.content as WeatherUiState.Content.Data
        assertEquals(TestData.week, content.forecast.days)
        assertEquals(TestData.now, content.forecast.lastUpdated)
        assertFalse(state.isRefreshing)
        assertNull(state.transientError)
    }

    @Test
    fun `offline relaunch with cache shows cached data plus a non-blocking error`() = runTest {
        val cached = TestData.cachedForecast()
        val vm = viewModel(FakeLocalDataSource(cached), FakeRemoteDataSource().apply { goOffline() })
        subscribe(vm)

        vm.onStart()

        val state = vm.uiState.value
        assertEquals(WeatherUiState.Content.Data(cached), state.content)
        assertEquals(RefreshError.NO_CONNECTION, state.transientError)

        vm.onTransientErrorShown()
        assertNull(vm.uiState.value.transientError)
        assertEquals(WeatherUiState.Content.Data(cached), vm.uiState.value.content)
    }

    @Test
    fun `offline with empty cache shows the empty state with the error, not a snackbar`() = runTest {
        val vm = viewModel(FakeLocalDataSource(), FakeRemoteDataSource().apply { goOffline() })
        subscribe(vm)

        vm.onStart()

        val state = vm.uiState.value
        assertEquals(WeatherUiState.Content.Empty(RefreshError.NO_CONNECTION), state.content)
        assertNull(state.transientError)
    }

    @Test
    fun `retry from the empty state recovers once the network is back`() = runTest {
        val remote = FakeRemoteDataSource().apply { goOffline() }
        val vm = viewModel(FakeLocalDataSource(), remote)
        subscribe(vm)
        vm.onStart()

        remote.failure = null
        vm.onRefresh()

        assertEquals(TestData.week, (vm.uiState.value.content as WeatherUiState.Content.Data).forecast.days)
    }

    @Test
    fun `onStart is idempotent across configuration changes`() = runTest {
        val remote = FakeRemoteDataSource()
        val vm = viewModel(FakeLocalDataSource(), remote)
        subscribe(vm)

        vm.onStart()
        vm.onStart()

        assertEquals(1, remote.requestedCoordinates.size)
    }
}
