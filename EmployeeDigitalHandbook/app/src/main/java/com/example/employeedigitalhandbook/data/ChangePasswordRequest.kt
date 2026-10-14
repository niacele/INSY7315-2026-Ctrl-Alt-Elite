package com.example.employeedigitalhandbook.data

import com.google.gson.annotations.SerializedName

data class ChangePasswordRequest(
    @SerializedName("UserId", alternate = ["userId"])
    val userId: Int,

    @SerializedName("CurrentPassword", alternate = ["currentPassword"])
    val currentPassword: String,

    @SerializedName("NewPassword", alternate = ["newPassword"])
    val newPassword: String
)