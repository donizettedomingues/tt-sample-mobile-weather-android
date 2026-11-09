package com.sample.weekweather.data.mapper

import com.sample.weekweather.data.remote.dto.DailyDto
import com.sample.weekweather.data.remote.dto.ForecastResponseDto
import com.sample.weekweather.domain.model.WeatherCondition
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private val json = Json { ignoreUnknownKeys = true }

class ForecastDtoMapperTest {

    private val mapper = ForecastDtoMapper(WeatherCodeMapper())

    @Test
    fun `adapts parallel daily arrays from a real-shaped response into domain days`() {
        // Trimmed copy of an actual Open-Meteo response (extra fields are ignored).
        val body = """
            {"latitude":45.5,"longitude":-73.56,"generationtime_ms":0.05,"utc_offset_seconds":-18000,
             "timezone":"America/Toronto","timezone_abbreviation":"GMT-5","elevation":37.0,
             "daily_units":{"time":"iso8601","weather_code":"wmo code","temperature_2m_max":"°C","temperature_2m_min":"°C","precipitation_probability_max":"%"},
             "daily":{"time":["2026-11-09","2026-11-10"],"weather_code":[3,61],
                      "temperature_2m_max":[8.4,6.1],"temperature_2m_min":[-1.2,2.0],
                      "precipitation_probability_max":[10,null]}}
        """.trimIndent()
        val dto = json.decodeFromString<ForecastResponseDto>(body)

        val days = mapper.toDomain(dto)

        assertEquals(2, days.size)
        assertEquals(LocalDate.of(2026, 11, 9), days[0].date)
        assertEquals(WeatherCondition.OVERCAST, days[0].condition)
        assertEquals(8.4, days[0].maxTemperatureC, 0.0)
        assertEquals(-1.2, days[0].minTemperatureC, 0.0)
        assertEquals(10, days[0].precipitationProbabilityPercent)
        assertEquals(WeatherCondition.RAIN, days[1].condition)
        assertEquals(null, days[1].precipitationProbabilityPercent)
    }

    @Test
    fun `drops days with missing temperatures or invalid dates instead of inventing values`() {
        val dto = ForecastResponseDto(
            latitude = 0.0,
            longitude = 0.0,
            daily = DailyDto(
                time = listOf("2026-11-09", "not-a-date", "2026-11-11", "2026-11-12"),
                weatherCode = listOf(0, 0, null, 1),
                temperatureMax = listOf(10.0, 11.0, 12.0, null),
                temperatureMin = listOf(1.0, 2.0, 3.0, 4.0),
            ),
        )

        val days = mapper.toDomain(dto)

        assertEquals(listOf(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 11)), days.map { it.date })
        assertEquals(WeatherCondition.UNKNOWN, days[1].condition) // null code
    }

    @Test
    fun `missing daily block yields an empty list`() {
        assertTrue(mapper.toDomain(ForecastResponseDto(latitude = 0.0, longitude = 0.0, daily = null)).isEmpty())
    }
}
