package com.joe.weatherapp // Make sure this matches your package name

import com.google.gson.annotations.SerializedName

// Overall response for 5-day / 3-hour forecast
data class ForecastResponse(
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<HourlyForecastItem>, // List of individual hourly forecasts
    val city: ForecastCity
)

// Represents a single hourly forecast item in the 'list' array
data class HourlyForecastItem(
    val dt: Long, // Time of data calculation, Unix, UTC
    val main: Main, // Reusing Main data class from WeatherResponse.kt
    val weather: List<Weather>, // Reusing Weather data class from WeatherResponse.kt
    val clouds: Clouds, // Reusing Clouds data class from WeatherResponse.kt
    val wind: Wind, // Reusing Wind data class from WeatherResponse.kt
    val visibility: Int,
    val pop: Double, // Probability of precipitation
    val sys: HourlySys, // Specific sys for hourly forecast
    @SerializedName("dt_txt")
    val dtTxt: String // Time of data forecasted, in YYYY-MM-DD HH:MM:SS format
)

// Specific sys for hourly forecast (simpler than current weather sys)
data class HourlySys(
    val pod: String // Part of the day (d = day, n = night)
)

// City information for the forecast
data class ForecastCity(
    val id: Long,
    val name: String,
    val coord: Coord, // Reusing Coord data class from WeatherResponse.kt
    val country: String,
    val population: Long,
    val timezone: Long,
    val sunrise: Long,
    val sunset: Long
)