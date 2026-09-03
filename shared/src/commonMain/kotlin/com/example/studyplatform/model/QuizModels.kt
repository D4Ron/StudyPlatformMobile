package com.example.studyplatform.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GenerateQuizRequest(
    val topic: String, val difficulty: String,
    val quizType: String, val questionCount: Int = 15,
    val guideId: String? = null
)

@Serializable
data class QuizResponse(
    val id: String, val title: String, val topic: String,
    val difficulty: String, val quizType: String,
    val questionCount: Int = 0, val questions: JsonElement? = null,
    val guideId: String? = null, val createdAt: String? = null
)

@Serializable
data class QuizListItem(
    val id: String, val title: String, val topic: String,
    val difficulty: String, val quizType: String,
    val questionCount: Int = 0, val createdAt: String? = null
)

@Serializable
data class SubmitQuizRequest(
    val quizId: String, val answers: List<AnswerSubmission>,
    val timeTakenSeconds: Int = 0
)

@Serializable
data class AnswerSubmission(val questionIndex: Int, val selectedAnswer: String)

@Serializable
data class QuizAttemptResponse(
    val id: String, val quizId: String, val quizTitle: String? = null,
    val score: Int = 0, val totalPoints: Int = 0,
    val correctCount: Int = 0, val totalQuestions: Int = 0,
    val percentageScore: Double = 0.0, val answers: JsonElement? = null,
    val timeTakenSeconds: Int = 0, val completedAt: String? = null
)
