package com.example.employeedigitalhandbook.data

import com.google.gson.annotations.SerializedName

data class QuizOption(
    @SerializedName("optionId", alternate = ["OptionId"])
    val optionId: Int = 0,

    @SerializedName("optionText", alternate = ["OptionText"])
    val optionText: String = "",

    @SerializedName("isCorrect", alternate = ["IsCorrect"])
    val isCorrect: Boolean = false
)

data class QuizQuestion(
    @SerializedName("questionId", alternate = ["QuestionId"])
    val questionId: Int = 0,

    @SerializedName("questionText", alternate = ["QuestionText"])
    val questionText: String = "",

    @SerializedName("quizId", alternate = ["QuizId"])
    val quizId: Int = 0,

    @SerializedName("options", alternate = ["Options"])
    val options: List<QuizOption> = emptyList()
)

data class Quiz(
    @SerializedName("quizId", alternate = ["QuizId"])
    val quizId: Int = 0,

    @SerializedName("title", alternate = ["Title"])
    val title: String = "",

    @SerializedName("score", alternate = ["Score"])
    val points: Double = 0.0,

    @SerializedName("passingScore", alternate = ["PassingScore"])
    val passPercentage: Int = 80,

    @SerializedName("estimateTime", alternate = ["EstimateTime"])
    val estimatedMinutes: String = "",

    @SerializedName("questions", alternate = ["Questions"])
    val questions: List<QuizQuestion> = emptyList()
)

data class QuizSubmission(
    @SerializedName("QuizId", alternate = ["quizId"])
    val quizId: Int,

    @SerializedName("ScoreFraction", alternate = ["scoreFraction"])
    val scoreFraction: String,

    @SerializedName("Percentage", alternate = ["percentage"])
    val percentage: Int,

    @SerializedName("Passed", alternate = ["passed"])
    val passed: Boolean,

    @SerializedName("CpdPointsEarned", alternate = ["cpdPointsEarned"])
    val cpdPointsEarned: Double,

    @SerializedName("UserId", alternate = ["userId"])
    val userId: Int?
)