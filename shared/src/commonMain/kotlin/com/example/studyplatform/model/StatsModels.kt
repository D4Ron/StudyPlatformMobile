package com.example.studyplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardStats(
    val totalXp: Int = 0, val totalStudyMinutes: Long = 0,
    val guidesCompleted: Int = 0, val quizzesTaken: Int = 0,
    val averageQuizScore: Double? = null,
    val xpByTopic: List<TopicXp>? = null,
    val recentActivity: List<RecentActivity>? = null
)

@Serializable
data class TopicXp(val topic: String, val xp: Long)

@Serializable
data class RecentActivity(
    val type: String, val description: String,
    val xpEarned: Int = 0, val timestamp: String? = null
)

@Serializable
data class LevelResponse(
    val level: Int = 1, val title: String = "",
    val currentXp: Int = 0, val xpForCurrentLevel: Int = 0,
    val xpForNextLevel: Int = 0, val progressPercent: Double = 0.0
)

@Serializable
data class BadgeResponse(
    val id: String, val code: String, val name: String,
    val description: String? = null, val category: String,
    val xpReward: Int = 0, val earned: Boolean = false,
    val earnedAt: String? = null, val progress: Double? = null
)
@Serializable
data class StudySessionRequest(
    val startTime: String,
    val endTime: String? = null,
    val topicId: String? = null,
    val focusScore: Int = 0,
    val activity: String? = null
)