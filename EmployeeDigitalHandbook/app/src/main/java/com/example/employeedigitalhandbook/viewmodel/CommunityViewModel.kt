package com.example.employeedigitalhandbook.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.employeedigitalhandbook.api.ApiClient.apiService
import com.example.employeedigitalhandbook.data.CommunityEvent
import com.example.employeedigitalhandbook.repositories.EventResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CommunityViewModel : ViewModel() {

    private val _eventsState = MutableLiveData<EventResult>()
    val eventsState: LiveData<EventResult> = _eventsState

    private var originalEventList: List<CommunityEvent> = emptyList()

    fun loadEvents() {
        viewModelScope.launch {
            try {
                val response = apiService.getCommunityEvents()
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!

                    if (apiResponse.success && apiResponse.data != null) {
                        originalEventList = apiResponse.data
                        _eventsState.value = EventResult.Success(originalEventList)
                    } else {
                        _eventsState.value = EventResult.Error(apiResponse.message ?: "Failed to load events")
                    }
                } else {
                    _eventsState.value = EventResult.Error("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                _eventsState.value = EventResult.Error("Network error: ${e.localizedMessage}")
            }
        }
    }

    fun filterEventsByCategory(categoryLabel: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val filtered = when (categoryLabel) {
            "Upcoming" -> {
                val calendar = Calendar.getInstance()
                val todayStr = dateFormat.format(calendar.time)

                calendar.add(Calendar.DAY_OF_YEAR, 7)
                val nextWeekStr = dateFormat.format(calendar.time)

                originalEventList.filter { event ->
                    val eventDate = event.formattedDate
                    eventDate != "TBA" && eventDate >= todayStr && eventDate <= nextWeekStr
                }
            }
            "Social" -> {
                originalEventList.filter { event ->
                    event.category == "0" || event.category.equals("Social", ignoreCase = true)
                }
            }
            "Wellness & Team" -> {
                originalEventList.filter { event ->
                    event.category == "1" || event.category == "2" ||
                            event.category.equals("Wellness", ignoreCase = true) ||
                            event.category.equals("TeamBuilding", ignoreCase = true)
                }
            }
            else -> originalEventList // "All"
        }

        _eventsState.value = EventResult.Success(filtered)
    }
}