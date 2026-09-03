package com.example.studyplatform.api

import com.example.studyplatform.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*

object QuizApi {
    /**
     * Starts generation and waits for it. Like guides, the endpoint hands back a job
     * rather than the quiz, so the request is not left open for the length of an AI call.
     */
    suspend fun generate(
        request: GenerateQuizRequest,
        onStatus: (String) -> Unit = {}
    ): QuizResponse {
        try {
            println("Generating quiz: topic=${request.topic}, difficulty=${request.difficulty}, count=${request.questionCount}")
            val job: JobResponseDto =
                ApiClient.client.post("/api/quizzes/generate") { setBody(request) }.body()

            val finished = JobPoller.await(job.id) { onStatus(it.status) }
            val quizId = finished.resultId
                ?: throw Exception("The quiz was generated but could not be located.")

            val quiz = getById(quizId)
            println("Quiz generated: ${quiz.id} — ${quiz.title}")
            return quiz
        } catch (e: JobFailedException) {
            throw e
        } catch (e: ApiException) {
            println("Quiz generation API error [${e.statusCode}]: ${e.rawBody}")
            when (e.statusCode) {
                502, 503, 504 -> throw Exception("The server is waking up. Please wait 30 seconds and try again.")
                else -> throw Exception("Could not generate the quiz. Please try again.")
            }
        } catch (e: Exception) {
            if (e.message?.contains("timed out") == true || e.message?.contains("Timeout") == true) {
                println("Quiz generation timeout: ${e.message}")
                throw Exception("The request timed out. The server might be starting up — please try again in a moment.")
            }
            println("Quiz generation error: ${e.message}")
            throw Exception("Could not generate the quiz. Please check your connection and try again.")
        }
    }

    suspend fun submit(request: SubmitQuizRequest): QuizAttemptResponse {
        try {
            return ApiClient.client.post("/api/quizzes/submit") { setBody(request) }.body()
        } catch (e: Exception) {
            println("Quiz submit error: ${e.message}")
            throw Exception("Could not submit your answers. Please try again.")
        }
    }

    suspend fun list(): List<QuizListItem> {
        try {
            return ApiClient.client.get("/api/quizzes").body()
        } catch (e: Exception) {
            println("Quiz list error: ${e.message}")
            return emptyList()
        }
    }

    suspend fun getById(id: String): QuizResponse {
        try {
            return ApiClient.client.get("/api/quizzes/$id").body()
        } catch (e: Exception) {
            println("Quiz fetch error: ${e.message}")
            throw Exception("Could not load the quiz. Please try again.")
        }
    }

    suspend fun getAttempts(quizId: String): List<QuizAttemptResponse> {
        try {
            return ApiClient.client.get("/api/quizzes/$quizId/attempts").body()
        } catch (e: Exception) {
            println("Quiz attempts error: ${e.message}")
            return emptyList()
        }
    }

    suspend fun getMyAttempts(): List<QuizAttemptResponse> {
        try {
            return ApiClient.client.get("/api/quizzes/attempts/mine").body()
        } catch (e: Exception) {
            println("My attempts error: ${e.message}")
            return emptyList()
        }
    }

    suspend fun delete(id: String) {
        try {
            ApiClient.client.delete("/api/quizzes/$id")
        } catch (e: Exception) {
            println("Quiz delete error: ${e.message}")
            throw Exception("Could not delete the quiz.")
        }
    }
}
