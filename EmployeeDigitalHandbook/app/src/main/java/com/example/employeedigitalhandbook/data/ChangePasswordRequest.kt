package com.example.employeedigitalhandbook.data

import com.google.gson.annotations.SerializedName

data class ChangePasswordRequest(
    @SerializedName("UserId")
    val userId: Int,

    @SerializedName("CurrentPassword")
    val currentPassword: String,

    @SerializedName("NewPassword")
    val newPassword: String
)