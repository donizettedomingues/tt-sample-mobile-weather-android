package com.sample.weekweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sample.weekweather.ui.theme.WeekWeatherTheme
import com.sample.weekweather.ui.weather.WeatherRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeekWeatherTheme {
                WeatherRoute()
            }
        }
    }
}
