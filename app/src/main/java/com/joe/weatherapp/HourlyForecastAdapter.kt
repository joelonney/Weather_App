package com.joe.weatherapp

import android.util.Log // Import Log for debugging
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joe.weatherapp.databinding.HourlyForecastItemBinding
import com.squareup.picasso.Picasso
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HourlyForecastAdapter(private var hourlyItems: List<HourlyForecastItem>) :
    RecyclerView.Adapter<HourlyForecastAdapter.HourlyViewHolder>() {

    private val TAG = "WeatherAppHourly" // Specific tag for adapter logs

    inner class HourlyViewHolder(private val binding: HourlyForecastItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyForecastItem) {
            // Log the raw Unix timestamp and the formatted time
            Log.d(TAG, "Binding item: raw_dt=${item.dt}, temp=${item.main.temp}")

            // Format time
            val timeFormatter = SimpleDateFormat("h a", Locale.getDefault()) // e.g., "3 PM"
            // If you prefer "00:00 AM" style: SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = Date(item.dt * 1000L) // Convert Unix timestamp (seconds) to milliseconds
            val formattedTime = timeFormatter.format(date)
            binding.textViewHourlyTime.text = formattedTime
            Log.d(TAG, "Formatted time for dt=${item.dt}: $formattedTime")

            // Format temperature
            val decimalFormat = DecimalFormat("#.#")
            val temperature = decimalFormat.format(item.main.temp)
            binding.textViewHourlyTemp.text = "${temperature}°"

            // Load weather icon
            val iconCode = item.weather[0].icon
            val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
            Picasso.get().load(iconUrl).into(binding.imageViewHourlyIcon)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val binding = HourlyForecastItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HourlyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        val item = hourlyItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = hourlyItems.size

    fun updateData(newHourlyItems: List<HourlyForecastItem>) {
        Log.d(TAG, "HourlyForecastAdapter updating data with ${newHourlyItems.size} items.")
        hourlyItems = newHourlyItems
        notifyDataSetChanged()
    }
}
