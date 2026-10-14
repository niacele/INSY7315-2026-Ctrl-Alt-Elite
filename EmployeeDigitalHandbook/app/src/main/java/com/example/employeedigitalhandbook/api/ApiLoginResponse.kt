package com.example.employeedigitalhandbook.api

import com.google.gson.annotations.SerializedName

data class ApiLoginResponse(
    @SerializedName("success", alternate = ["Success"])
    val success: Boolean = false,

    @SerializedName("message", alternate = ["Message"])
    val message: String? = null,

    @SerializedName("data", alternate = ["Data"])
    val data: ApiUserData? = null,

    // Fallback fields in case the backend sends flat JSON directly
    @SerializedName("userId", alternate = ["UserId", "id", "Id"])
    val userId: Int? = null,

    @SerializedName("email", alternate = ["Email"])
    val email: String? = null,

    @SerializedName("name", alternate = ["Name", "fullName", "FullName"])
    val name: String? = null,

    @SerializedName("token", alternate = ["Token"])
    val token: String? = null
)

data class ApiUserData(
    @SerializedName("userId", alternate = ["UserId", "id", "Id"])
    val userId: Int = 0,

    @SerializedName("email", alternate = ["Email"])
    val email: String? = null,

    @SerializedName("fullName", alternate = ["FullName", "name", "Name"])
    val fullName: String? = null,

    @SerializedName("token", alternate = ["Token"])
    val token: String? = null
)