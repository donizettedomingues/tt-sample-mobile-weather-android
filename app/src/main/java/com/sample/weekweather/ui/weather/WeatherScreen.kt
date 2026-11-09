package com.sample.weekweather.ui.weather

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sample.weekweather.R
import com.sample.weekweather.domain.model.DailyForecast
import com.sample.weekweather.domain.model.RefreshError
import com.sample.weekweather.domain.model.WeeklyForecast
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

/** Stateful entry point: wires the ViewModel and the permission prompt. */
@Composable
fun WeatherRoute(viewModel: WeatherViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionPromptShown by rememberSaveable { mutableStateOf(false) }

    // Whatever the answer, start: a denial simply means the default city is used.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.onStart() }

    LaunchedEffect(Unit) {
        val granted = LOCATION_PERMISSIONS.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        when {
            granted || permissionPromptShown -> viewModel.onStart()
            else -> {
                permissionPromptShown = true
                permissionLauncher.launch(LOCATION_PERMISSIONS)
            }
        }
    }

    WeatherScreen(
        state = uiState,
        onRefresh = viewModel::onRefresh,
        onTransientErrorShown = viewModel::onTransientErrorShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    state: WeatherUiState,
    onRefresh: () -> Unit,
    onTransientErrorShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val transientMessage = state.transientError?.let { stringResource(it.messageRes) }
    LaunchedEffect(transientMessage) {
        if (transientMessage != null) {
            snackbarHostState.showSnackbar(transientMessage)
            onTransientErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.isRefreshing) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val content = state.content) {
                WeatherUiState.Content.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is WeatherUiState.Content.Empty -> EmptyState(content.error, onRetry = onRefresh)
                is WeatherUiState.Content.Data -> ForecastContent(content.forecast, state.isRefreshing)
            }
        }
    }
}

@Composable
private fun EmptyState(error: RefreshError?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "📭", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(error?.messageRes ?: R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

@Composable
private fun ForecastContent(forecast: WeeklyForecast, isRefreshing: Boolean) {
    Column(Modifier.fillMaxSize()) {
        if (isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { ForecastHeader(forecast) }
            items(forecast.days, key = { it.date.toString() }) { day -> DayRow(day) }
        }
    }
}

@Composable
private fun ForecastHeader(forecast: WeeklyForecast) {
    val location = forecast.location
    val placeName = location.cityName ?: stringResource(R.string.current_location)
    val lastUpdated = remember(forecast.lastUpdated) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(forecast.lastUpdated)
    }
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(placeName, style = MaterialTheme.typography.headlineSmall)
        if (location.isFallback) {
            Text(
                text = stringResource(R.string.fallback_location_notice, placeName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Text(
            text = stringResource(R.string.last_updated, lastUpdated),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayRow(day: DailyForecast) {
    val dayName = remember(day.date) {
        day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
    val dateText = remember(day.date) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(day.date)
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(day.condition.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(dayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$dateText · ${stringResource(day.condition.labelRes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(
                        R.string.temperature_range,
                        day.maxTemperatureC.roundToInt(),
                        day.minTemperatureC.roundToInt(),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                day.precipitationProbabilityPercent?.let { chance ->
                    Text(
                        text = stringResource(R.string.precipitation_chance, chance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
