package com.joe.weatherapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.joe.weatherapp.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherApiService: WeatherApiService
    private lateinit var hourlyForecastAdapter: HourlyForecastAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val API_KEY = "79dae0b85dd7fd0e818cfb921d2298ec"
    private val BASE_URL = "https://api.openweathermap.org/"
    private val TAG = "WeatherApp"

    // Constants for bottom sheet states and animation
    private val COLLAPSED_HEIGHT_PERCENT = 0.35f // Initial visible height
    private val EXPANDED_HEIGHT_PERCENT = 0.95f // Almost full screen
    private val ANIMATION_DURATION = 300L // milliseconds
    private var isBottomSheetExpanded = false // Track current state

    private var initialY: Float = 0f // For tracking touch start Y position

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        Log.d(TAG, "Permissions granted: Fine=$fineLocationGranted, Coarse=$coarseLocationGranted")

        if (fineLocationGranted || coarseLocationGranted) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Location permission denied. Showing weather for London.", Toast.LENGTH_LONG).show()
            fetchWeatherData("London")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        weatherApiService = retrofit.create(WeatherApiService::class.java)

        // Initialize RecyclerView and Adapter for the bottom sheet
        // Access recyclerViewHourlyForecast via weatherCardContainer
        hourlyForecastAdapter = HourlyForecastAdapter(emptyList())
        binding.weatherCardContainer.recyclerViewHourlyForecast.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = hourlyForecastAdapter
        }

        binding.buttonSearch.setOnClickListener {
            val city = binding.editTextCityName.text.toString().trim()
            if (city.isNotEmpty()) {
                fetchWeatherData(city)
            } else {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
            }
        }

        binding.imageViewRefresh.setOnClickListener {
            Log.d(TAG, "Refresh button clicked.")
            val currentCityInEditText = binding.editTextCityName.text.toString().trim()
            if (currentCityInEditText.isNotEmpty()) {
                fetchWeatherData(currentCityInEditText)
            } else {
                checkLocationPermissions()
            }
            Toast.makeText(this, "Refreshing weather...", Toast.LENGTH_SHORT).show()
        }

        binding.editTextCityName.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val city = binding.editTextCityName.text.toString().trim()
                if (city.isNotEmpty()) {
                    fetchWeatherData(city)
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                } else {
                    Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        // Set up touch listener for the weatherCardContainer
        binding.weatherCardContainer.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaY = event.rawY - initialY
                        if (deltaY < -100) { // Swiped up significantly
                            expandBottomSheet()
                        } else if (deltaY > 100) { // Swiped down significantly
                            collapseBottomSheet()
                        }
                        return true
                    }
                }
                return false
            }
        })

        checkLocationPermissions()
    }

    // Function to expand the bottom sheet
    private fun expandBottomSheet() {
        if (isBottomSheetExpanded) return

        val constraintLayout = binding.mainConstraintLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        constraintSet.connect(
            binding.weatherCardContainer.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            0
        )
        constraintSet.setVerticalBias(binding.weatherCardContainer.id, 0.0f)
        constraintSet.constrainPercentHeight(binding.weatherCardContainer.id, EXPANDED_HEIGHT_PERCENT)

        val transition = androidx.transition.TransitionSet()
            .addTransition(androidx.transition.ChangeBounds())
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
        androidx.transition.TransitionManager.beginDelayedTransition(constraintLayout, transition)
        constraintSet.applyTo(constraintLayout)

        isBottomSheetExpanded = true
        Log.d(TAG, "Bottom sheet expanded.")
    }

    // Function to collapse the bottom sheet
    private fun collapseBottomSheet() {
        if (!isBottomSheetExpanded) return

        val constraintLayout = binding.mainConstraintLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        constraintSet.connect(
            binding.weatherCardContainer.id,
            ConstraintSet.TOP,
            binding.linearLayoutCityInput.id,
            ConstraintSet.BOTTOM,
            0
        )
        constraintSet.setVerticalBias(binding.weatherCardContainer.id, 1.0f)
        constraintSet.constrainPercentHeight(binding.weatherCardContainer.id, COLLAPSED_HEIGHT_PERCENT)


        val transition = androidx.transition.TransitionSet()
            .addTransition(androidx.transition.ChangeBounds())
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
        androidx.transition.TransitionManager.beginDelayedTransition(constraintLayout, transition)
        constraintSet.applyTo(constraintLayout)

        isBottomSheetExpanded = false
        Log.d(TAG, "Bottom sheet collapsed.")
    }


    private fun checkLocationPermissions() {
        Log.d(TAG, "Checking location permissions...")
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Fine location permission already granted.")
                getLastLocation()
            }
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Coarse location permission already granted.")
                getLastLocation()
            }
            else -> {
                Log.d(TAG, "Requesting location permissions...")
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun getLastLocation() {
        Log.d(TAG, "Attempting to get last known location...")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permissions not granted when calling getLastLocation. This should not happen.")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d(TAG, "Location received: Lat=${location.latitude}, Lon=${location.longitude}")
                    CoroutineScope(Dispatchers.IO).launch {
                        val cityName = getCityNameFromLocation(location.latitude, location.longitude)
                        if (cityName != null) {
                            Log.d(TAG, "City name from geocoder: $cityName")
                            runOnUiThread {
                                binding.editTextCityName.setText(cityName)
                                fetchWeatherData(cityName)
                            }
                        } else {
                            Log.w(TAG, "Could not determine city from location. Geocoder returned null.")
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Could not determine city from location. Showing weather for London.", Toast.LENGTH_LONG).show()
                                fetchWeatherData("London")
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Last known location is null. Location services might be off or no recent location recorded.")
                    Toast.makeText(this, "Unable to get current location. Showing weather for London.", Toast.LENGTH_LONG).show()
                    fetchWeatherData("London")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get location: ${e.message}", e)
                Toast.makeText(this, "Failed to get location: ${e.message}. Showing weather for London.", Toast.LENGTH_LONG).show()
                fetchWeatherData("London")
            }
    }

    private suspend fun getCityNameFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
            }
            val cityName = addresses?.firstOrNull()?.locality
            Log.d(TAG, "Geocoder result for ($latitude, $longitude): $cityName")
            cityName
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocoding failed: ${e.message}", e)
            null
        }
    }

    private fun fetchWeatherData(cityName: String) {
        if (API_KEY == "YOUR_OPENWEATHERMAP_API_KEY" || API_KEY.isEmpty()) {
            Toast.makeText(this, "Please replace YOUR_OPENWEATHERMAP_API_KEY with your actual key in MainActivity.kt", Toast.LENGTH_LONG).show()
            Log.e(TAG, "API Key is not set!")
            return
        }
        Log.d(TAG, "Fetching weather for city: $cityName")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentWeatherResponse = weatherApiService.getCurrentWeather(cityName, API_KEY)
                val hourlyForecastResponse = weatherApiService.getHourlyForecast(cityName, API_KEY)

                if (currentWeatherResponse.isSuccessful && currentWeatherResponse.body() != null &&
                    hourlyForecastResponse.isSuccessful && hourlyForecastResponse.body() != null) {

                    val weatherData = currentWeatherResponse.body()
                    val forecastData = hourlyForecastResponse.body()

                    Log.d(TAG, "Weather data fetched successfully for ${weatherData?.name}")
                    Log.d(TAG, "Forecast data fetched successfully for ${forecastData?.city?.name}")

                    runOnUiThread {
                        updateUI(weatherData)
                        forecastData?.let {
                            val hourlyItemsToShow = it.list.take(8)
                            hourlyForecastAdapter.updateData(hourlyItemsToShow)
                            Log.d(TAG, "Hourly forecast updated with ${hourlyItemsToShow.size} items.")
                        }
                    }
                } else {
                    val currentErrorMessage = currentWeatherResponse.errorBody()?.string() ?: "Unknown current weather error"
                    val hourlyErrorMessage = hourlyForecastResponse.errorBody()?.string() ?: "Unknown hourly forecast error"
                    Log.e(TAG, "API Error: Current: $currentErrorMessage, Hourly: $hourlyErrorMessage")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error fetching weather for $cityName: $currentErrorMessage", Toast.LENGTH_LONG).show()
                        binding.textViewTemperature.text = "--°C"
                        binding.textViewDescription.text = "Failed to load weather"
                        binding.imageViewWeatherIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                        hourlyForecastAdapter.updateData(emptyList())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network/API call exception: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.textViewTemperature.text = "--°C"
                    binding.textViewDescription.text = "Check internet connection"
                    binding.imageViewWeatherIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                    hourlyForecastAdapter.updateData(emptyList())
                }
            }
        }
    }

    private fun updateUI(weatherResponse: WeatherResponse?) {
        weatherResponse?.let {
            val decimalFormat = DecimalFormat("#.#")
            val temperatureCelsius = decimalFormat.format(it.main.temp)

            binding.textViewDescription.text = "${it.weather[0].description.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }} in ${it.name}"

            binding.textViewTemperature.text = "${temperatureCelsius}°C"

            val iconCode = it.weather[0].icon
            val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
            Picasso.get().load(iconUrl).into(binding.imageViewWeatherIcon)
        }
    }

    private fun formatUnixTimestamp(timestamp: Long): String {
        val date = Date(timestamp * 1000L)
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }
}
