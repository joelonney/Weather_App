package com.joe.weatherapp

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    // Endpoint for current weather data
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") cityName: String, // Query parameter for city name
        @Query("appid") apiKey: String, // Query parameter for your API key
        @Query("units") units: String = "metric" // Optional: "metric" for Celsius, "imperial" for Fahrenheit
    ): Response<WeatherResponse>

    // NEW: Endpoint for 5-day / 3-hour forecast data
    @GET("data/2.5/forecast")
    suspend fun getHourlyForecast(
        @Query("q") cityName: String, // Query parameter for city name
        @Query("appid") apiKey: String, // Query parameter for your API key
        @Query("units") units: String = "metric" // Optional: "metric" for Celsius, "imperial" for Fahrenheit
    ): Response<ForecastResponse> // Expected response type is our new ForecastResponse data class
}
