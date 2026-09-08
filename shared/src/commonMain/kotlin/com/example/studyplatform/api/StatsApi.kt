package com.example.studyplatform.api

import com.example.studyplatform.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*

object StatsApi {
    suspend fun getDashboard(): DashboardStats =
        ApiClient.client.get("/api/stats/dashboard").body()

    suspend fun logSession(request: StudySessionRequest): MessageResponse =
        ApiClient.client.post("/api/stats/sessions") { setBody(request) }.body()

    suspend fun getStreak(): StreakResponse =
        ApiClient.client.get("/api/stats/streak").body()

    suspend fun getLevel(): LevelResponse =
        ApiClient.client.get("/api/gamification/level").body()

    suspend fun getBadges(): List<BadgeResponse> =
        ApiClient.client.get("/api/gamification/badges").body()

    suspend fun checkBadges(): MessageResponse =
        ApiClient.client.post("/api/gamification/badges/check").body()
}
