package com.sample.weekweather.ui.weather

import androidx.annotation.StringRes
import com.sample.weekweather.R
import com.sample.weekweather.domain.model.RefreshError
import com.sample.weekweather.domain.model.WeatherCondition

/** Presentation-only mapping; keeps resources out of the domain. */
internal val WeatherCondition.emoji: String
    get() = when (this) {
        WeatherCondition.CLEAR -> "☀️"
        WeatherCondition.MAINLY_CLEAR -> "🌤️"
        WeatherCondition.PARTLY_CLOUDY -> "⛅"
        WeatherCondition.OVERCAST -> "☁️"
        WeatherCondition.FOG -> "🌫️"
        WeatherCondition.DRIZZLE -> "🌦️"
        WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS -> "🌧️"
        WeatherCondition.FREEZING_RAIN -> "🧊"
        WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS -> "🌨️"
        WeatherCondition.THUNDERSTORM -> "⛈️"
        WeatherCondition.UNKNOWN -> "❔"
    }

@get:StringRes
internal val WeatherCondition.labelRes: Int
    get() = when (this) {
        WeatherCondition.CLEAR -> R.string.condition_clear
        WeatherCondition.MAINLY_CLEAR -> R.string.condition_mainly_clear
        WeatherCondition.PARTLY_CLOUDY -> R.string.condition_partly_cloudy
        WeatherCondition.OVERCAST -> R.string.condition_overcast
        WeatherCondition.FOG -> R.string.condition_fog
        WeatherCondition.DRIZZLE -> R.string.condition_drizzle
        WeatherCondition.RAIN -> R.string.condition_rain
        WeatherCondition.FREEZING_RAIN -> R.string.condition_freezing_rain
        WeatherCondition.SNOW -> R.string.condition_snow
        WeatherCondition.RAIN_SHOWERS -> R.string.condition_rain_showers
        WeatherCondition.SNOW_SHOWERS -> R.string.condition_snow_showers
        WeatherCondition.THUNDERSTORM -> R.string.condition_thunderstorm
        WeatherCondition.UNKNOWN -> R.string.condition_unknown
    }

@get:StringRes
internal val RefreshError.messageRes: Int
    get() = when (this) {
        RefreshError.NO_CONNECTION -> R.string.error_no_connection
        RefreshError.SERVER -> R.string.error_server
        RefreshError.UNKNOWN -> R.string.error_unknown
    }
