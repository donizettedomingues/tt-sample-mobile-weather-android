package com.sample.weekweather.data.mapper

import com.sample.weekweather.domain.model.WeatherCondition
import javax.inject.Inject

/**
 * Translates WMO weather interpretation codes (used by Open-Meteo) into our own enum,
 * so no other layer needs to know the numeric codes.
 * Reference: https://open-meteo.com/en/docs ("WMO Weather interpretation codes").
 */
class WeatherCodeMapper @Inject constructor() {

    fun map(code: Int?): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        1 -> WeatherCondition.MAINLY_CLEAR
        2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.OVERCAST
        45, 48 -> WeatherCondition.FOG
        51, 53, 55 -> WeatherCondition.DRIZZLE
        56, 57, 66, 67 -> WeatherCondition.FREEZING_RAIN
        61, 63, 65 -> WeatherCondition.RAIN
        71, 73, 75, 77 -> WeatherCondition.SNOW
        80, 81, 82 -> WeatherCondition.RAIN_SHOWERS
        85, 86 -> WeatherCondition.SNOW_SHOWERS
        95, 96, 99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.UNKNOWN
    }
}
