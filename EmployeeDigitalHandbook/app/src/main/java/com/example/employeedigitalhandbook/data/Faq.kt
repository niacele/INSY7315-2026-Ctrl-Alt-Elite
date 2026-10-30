package com.example.employeedigitalhandbook.data

import com.google.gson.annotations.SerializedName

data class Faq(
    @SerializedName("faqId") val faqId: Int = 0,
    @SerializedName("topicKey") val topicKey: String = "",
    @SerializedName("question") val question: String = "",
    @SerializedName("answer") val answer: String = "",
    @SerializedName("category") val category: String = ""
)