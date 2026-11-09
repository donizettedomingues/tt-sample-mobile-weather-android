package com.sample.weekweather.data.mapper

import com.sample.weekweather.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeMapperTest {

    private val mapper = WeatherCodeMapper()

    @Test
    fun `maps representative WMO codes to conditions`() {
        val expected = mapOf(
            0 to WeatherCondition.CLEAR,
            2 to WeatherCondition.PARTLY_CLOUDY,
            3 to WeatherCondition.OVERCAST,
            48 to WeatherCondition.FOG,
            55 to WeatherCondition.DRIZZLE,
            63 to WeatherCondition.RAIN,
            67 to WeatherCondition.FREEZING_RAIN,
            75 to WeatherCondition.SNOW,
            81 to WeatherCondition.RAIN_SHOWERS,
            86 to WeatherCondition.SNOW_SHOWERS,
            99 to WeatherCondition.THUNDERSTORM,
        )
        expected.forEach { (code, condition) -> assertEquals("code $code", condition, mapper.map(code)) }
    }

    @Test
    fun `unknown or missing codes map to UNKNOWN`() {
        assertEquals(WeatherCondition.UNKNOWN, mapper.map(42))
        assertEquals(WeatherCondition.UNKNOWN, mapper.map(null))
    }
}
