package com.joe.weatherapp // This should be the ONLY package declaration

import com.google.gson.annotations.SerializedName

// Main response object from the API
data class WeatherResponse(
    val coord: Coord,
    val weather: List<Weather>,
    val main: Main,
    val wind: Wind,
    val clouds: Clouds,
    val sys: Sys,
    val timezone: Long,
    val id: Long,
    val name: String, // City name
    val cod: Int,
    val dt: Long // Time of data calculation, Unix, UTC
)

// Represents the 'coord' object in JSON
data class Coord(
    val lon: Double,
    val lat: Double
)

// Represents an item in the 'weather' array in JSON
data class Weather(
    val id: Int,
    val main: String, // e.g., "Clear", "Clouds"
    val description: String, // e.g., "clear sky", "few clouds"
    val icon: String // e.g., "01d", "04n" - used to get the icon image URL
)

// Represents the 'main' object in JSON
data class Main(
    val temp: Double, // Current temperature
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("temp_min")
    val tempMin: Double,
    @SerializedName("temp_max")
    val tempMax: Double,
    val pressure: Int,
    val humidity: Int
)

// Represents the 'wind' object in JSON
data class Wind(
    val speed: Double,
    val deg: Int
)

// Represents the 'clouds' object in JSON
data class Clouds(
    val all: Int
)

// Represents the 'sys' object in JSON (contains sunrise/sunset, country code)
data class Sys(
    val type: Int,
    val id: Long,
    val country: String,
    val sunrise: Long, // Unix, UTC
    val sunset: Long // Unix, UTC
)