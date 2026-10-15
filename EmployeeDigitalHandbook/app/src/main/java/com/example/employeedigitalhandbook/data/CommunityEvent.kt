package com.example.employeedigitalhandbook.data

import com.google.gson.annotations.SerializedName

data class CommunityEvent(
    @SerializedName("eventId") val eventId: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("description", alternate = ["Description", "details", "Details"])
    val description: String = "",
    @SerializedName("eventDate", alternate = ["date", "eventDateTime", "dateTime", "Date"])
    val rawDateTime: String? = null,
    @SerializedName("location") val location: String = "",
    @SerializedName("eventCategory", alternate = ["category", "Category", "EventCategory"])
    val category: String = ""
) {
    val formattedDate: String
        get() {
            val dt = rawDateTime
            if (dt.isNullOrBlank()) return "TBA"

            return if (dt.contains("T")) {
                dt.substringBefore("T")
            } else if (dt.contains(" ")) {
                dt.substringBefore(" ")
            } else {
                dt
            }
        }

    val formattedTime: String
        get() {
            val dt = rawDateTime
            if (dt.isNullOrBlank()) return "TBA"

            val timePart = when {
                dt.contains("T") -> dt.substringAfter("T")
                dt.contains(" ") -> dt.substringAfter(" ")
                else -> null
            }

            return if (!timePart.isNullOrBlank()) {
                timePart.take(5)
            } else {
                "TBA"
            }
        }
}