# Week Weather (Android)

A small Android app that shows the **daily forecast for the coming week at the device's location**, built for the Trackforce Mobile Challenge (Weekly Weather).

- Kotlin, Jetpack Compose (Material 3)
- MVVM + use cases (Clean Architecture: `ui` / `domain` / `data`)
- Dagger Hilt for dependency injection
- Retrofit + kotlinx.serialization for the [Open-Meteo](https://open-meteo.com/en/docs) API (**no API key needed**)
- Room for the offline cache, FusedLocationProvider for the location
- 22 JVM unit tests (mappers, cache policy, repository, use cases, ViewModel)

## How to run

### Requirements

| Tool | Version |
|------|---------|
| JDK | 17 or newer (Android Studio's bundled JBR 17/21 works) |
| Android Studio | Ladybug (2024.2) or newer |
| Android SDK | Platform 35 (compileSdk / targetSdk 35), minSdk 26 |
| Gradle | 8.11.1 via the wrapper (no local install needed) |
| AGP / Kotlin | 8.7.3 / 2.1.0 (see `gradle/libs.versions.toml`) |

The build only uses `google()` and `mavenCentral()`. No API key, `local.properties` entry or secret is needed.

### Android Studio

1. **File → Open** and select this folder. Let Gradle sync.
2. Pick an emulator **with Google Play services** (a "Google APIs" or "Google Play" system image) so FusedLocationProvider works, or a real device.
3. Run the **app** configuration.
4. Emulator only: set a location in **Extended controls (…) → Location** before opening the app, otherwise the emulator may have no fix and the app uses the default city (see below).

### Command line

```bash
./gradlew assembleDebug                 # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                  # installs on the connected device/emulator
adb shell am start -n com.sample.weekweather/.MainActivity
```

### Tests

```bash
./gradlew testDebugUnitTest
```

Reports: `app/build/reports/tests/testDebugUnitTest/index.html`. In Android Studio, right-click `app/src/test` → **Run 'Tests in…'**.

## What the app does

- Asks for location permission on first launch (approximate is enough).
- Shows the place ("Current location" or the default city), a **"Last updated"** time and one card per day with: weekday and date, a weather icon and description, max / min temperature and the chance of precipitation.
- The toolbar **Refresh** button forces a new fetch.

## Location permission fallback

If the permission is **denied**, location services are off, or no fix arrives within 5 seconds, the app shows the forecast for the **default city: Montréal, QC (45.5019, -73.5674)**. The screen says so under the place name ("Location unavailable or permission denied — showing Montréal (default city)").

The app asks once. After a denial it does not ask again on each launch; the user can grant the permission in system settings, and the next refresh then uses the device location. The fallback lives in `data/location/DefaultCity.kt` and `data/location/FusedLocationProvider.kt`.

## Offline behaviour

The Room cache is the **single source of truth**: the UI only ever observes the cache, and the network only ever *writes into* it.

| # | Acceptance criterion | How it is implemented |
|---|---|---|
| 1 | On open with network: fetch, show and save the forecast | `WeatherRoute` calls `WeatherViewModel.onStart()` once the permission prompt is answered → `RefreshWeeklyForecastUseCase` → `OfflineFirstWeatherRepository.refresh()` saves into Room → Room re-emits → the screen updates. |
| 2 | Killed and relaunched offline: the last saved forecast shows | The forecast is stored in a SQLite DB (`week-weather.db`), not in memory. On launch, `uiState` observes the DB right away, so cached data shows even before the (failing) refresh finishes. |
| 3 | A "last updated" time is shown | `WeeklyForecast.lastUpdated` is the time of the last *successful* fetch, stored with the cache (`forecast_meta.fetchedAtEpochMillis`) and shown in the header. |
| 4 | Empty cache + no network: clear empty/error state, no crash | `WeatherUiState.Content.Empty` shows "No forecast yet", the reason (e.g. "No internet connection") and a **Try again** button. |
| 5 | Failed refresh keeps the cached data and shows a non-blocking error | The repository never touches the cache on failure (it also refuses to overwrite it with an empty response). The ViewModel turns the failure into `transientError`, shown as a **snackbar** over the cached forecast. |

To try it: open the app online, wait for the forecast, swipe it away from Recents (or `adb shell am force-stop com.sample.weekweather`), switch on airplane mode and open it again. For criterion 4, clear the app's storage while offline.

## Architecture

```
┌───────────────────────────── ui ─────────────────────────────┐
│ MainActivity → WeatherRoute / WeatherScreen (Compose)        │
│                    ▲ observes StateFlow<WeatherUiState>      │
│                WeatherViewModel (@HiltViewModel)             │
└────────────────────────────┬─────────────────────────────────┘
                             │ calls
┌─────────────────────────── domain ───────────────────────────┐
│ ObserveWeeklyForecastUseCase   RefreshWeeklyForecastUseCase  │
│ WeatherRepository (interface)  LocationProvider (interface)  │
│ Models: WeeklyForecast, DailyForecast, RefreshResult …       │
└────────────────────────────┬─────────────────────────────────┘
                             │ implemented by
┌──────────────────────────── data ────────────────────────────┐
│ OfflineFirstWeatherRepository ── CachePolicy (strategy)      │
│    ├── ForecastRemoteDataSource → OpenMeteoApi (Retrofit)    │
│    │       └── ForecastDtoMapper / WeatherCodeMapper         │
│    └── ForecastLocalDataSource → ForecastDao (Room)          │
│            └── ForecastEntityMapper                          │
│ FusedLocationProvider (Play services) → DefaultCity fallback │
└──────────────────────────────────────────────────────────────┘
          di/: NetworkModule, DatabaseModule, DataModule (Hilt)
```

Data flow:

1. **Read path:** Room `Flow<CachedForecast?>` → `ForecastEntityMapper` → `WeatherRepository.observeForecast()` → `ObserveWeeklyForecastUseCase` → `WeatherViewModel.uiState` (combined with the refresh status) → Compose.
2. **Write path:** `onStart()` / `onRefresh()` → `RefreshWeeklyForecastUseCase` (gets the location) → `repository.refresh(location, force)` → `CachePolicy` decides → Open-Meteo → `ForecastDtoMapper` → `dao.replace(...)` in one transaction → the read path emits.

The `domain` package has no Android, Room, Retrofit or Play services imports, so the use cases and models are plain Kotlin and easy to test.

## Design patterns

| Kind | Pattern | Where | Why it fits |
|---|---|---|---|
| **Creational** | **Factory** (built on Retrofit/OkHttp **Builders**) | `app/src/main/java/com/sample/weekweather/data/remote/OpenMeteoApiFactory.kt`, used by `di/NetworkModule.kt` | One place decides how the HTTP client is built (timeouts, JSON settings, base URL), so every caller, including a future mock-server test, gets the same configured API. |
| **Structural** | **Adapter / Mapper** | `data/mapper/ForecastDtoMapper.kt`, `data/mapper/ForecastEntityMapper.kt`, `data/mapper/WeatherCodeMapper.kt` | Open-Meteo returns parallel arrays and WMO codes; Room stores flat rows. The adapters turn both into one domain model, so no other layer knows those formats. |
| **Structural** | **Facade** (Repository) | `data/repository/OfflineFirstWeatherRepository.kt` | Hides the API, the Room cache and the cache policy behind two methods (`observeForecast`, `refresh`). |
| **Behavioral** | **Observer** (StateFlow / Room Flow) | `ui/weather/WeatherViewModel.kt` (`uiState`), `data/local/ForecastDao.kt` (`observeCachedForecast`) | The UI observes state instead of being pushed data: a refresh writes to the DB, Room notifies its observers and the new state flows to the screen. This is what makes offline-first simple. |
| **Behavioral** | **Strategy** | `data/policy/CachePolicy.kt` (`AlwaysFetchPolicy`, `MaxAgeCachePolicy`), selected in `di/DataModule.kt` | "When should we hit the network?" can change without touching the repository. Production uses `AlwaysFetchPolicy` because the challenge asks for a fetch on every open. |

All paths above are under `app/src/main/java/com/sample/weekweather/`.

## Weather provider

[Open-Meteo](https://open-meteo.com/) forecast API, open-access endpoint, **no API key**:

```
GET https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}
    &daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max
    &timezone=auto&forecast_days=7
```

`timezone=auto` makes the dates local to the forecast location. The base URL is the `OPEN_METEO_BASE_URL` BuildConfig field in `app/build.gradle.kts`. Open-Meteo's free tier is for non-commercial use; a production app would need its commercial plan or another provider.

## Tests

`app/src/test/java/com/sample/weekweather/` — 22 tests, plain JVM (no emulator), using in-memory fakes (`testing/Fakes.kt`) instead of mocks:

| Test class | What it checks |
|---|---|
| `ui/weather/WeatherViewModelTest` | Online start shows data; offline relaunch with cache shows cached data **plus** a snackbar error; empty cache + offline shows the empty state; retry recovers; `onStart()` is idempotent. |
| `data/repository/OfflineFirstWeatherRepositoryTest` | Success persists with location and fetch time; failure keeps the cache; empty cache failure; an empty response never wipes the cache; cache policy skip vs. forced refresh. |
| `domain/usecase/WeeklyForecastUseCasesTest` | The refresh uses the device location or the default city; observers get new data after a refresh (Turbine). |
| `data/mapper/ForecastDtoMapperTest` | Parses a real-shaped Open-Meteo response; drops days with missing values instead of showing "0°". |
| `data/mapper/WeatherCodeMapperTest` | WMO code → condition, unknown codes. |
| `data/policy/MaxAgeCachePolicyTest` | Empty, fresh, stale and moved-location cases. |

`kotlinx-coroutines-test` replaces `Dispatchers.Main`, and a fixed `java.time.Clock` makes "last updated" deterministic.

## Trade-offs and decisions

- **Single Gradle module.** The layers are packages, not modules. For an app this size, modules would add build setup without much benefit. The `domain` package has no Android imports, so splitting it out later is mechanical.
- **One cached forecast.** The cache keeps only the last successful forecast (one location). That is enough for "show me the week here" and keeps the schema simple.
- **Always fetch on open.** This matches acceptance criterion 1. `MaxAgeCachePolicy` is ready if we want to save data later (for example when refreshing on every resume).
- **No reverse geocoding.** Open-Meteo's forecast endpoint does not return a place name, so the device location is labelled "Current location". The default city has a name.
- **Emoji instead of icon assets** for weather conditions. They have no licensing or density issues, but look different on each device.
- **Errors are typed** (`RefreshError`), not strings. The UI picks the message, so it can be translated.
- **No pull-to-refresh.** A toolbar button avoids the experimental Material 3 pull-to-refresh API.

## What I would do next

- Instrumented tests: a Room DAO test (transaction, cascade) and a Compose UI test of the three screen states; a MockWebServer test of `OpenMeteoApiFactory` with a recorded response.
- Background refresh with WorkManager (periodic, network-constrained), so the cache is fresh before the user opens the app.
- Reverse geocoding (or Open-Meteo's geocoding API) and a manual city search, as an alternative fallback to the fixed default city.
- An explicit "you are offline" banner driven by `ConnectivityManager`, plus showing the cache's age ("3 h ago").
- Split into `:core:domain`, `:core:data`, `:feature:weather` modules once a second feature arrives; add detekt/ktlint and a GitHub Actions workflow running `./gradlew testDebugUnitTest lint`.
- Accessibility pass (content descriptions for weather icons, TalkBack order) and localisation (strings are already in resources).
