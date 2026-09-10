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

/**
 * Consecutive days studied.
 *
 * `studiedToday` is separate from `currentStreak` deliberately: a streak of 4 means
 * something different at 9am with nothing logged yet than it does at 9pm having already
 * studied, and the count alone cannot tell them apart.
 */
@Serializable
data class StreakResponse(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val daysStudied: Int = 0,
    val studiedToday: Boolean = false,
    val lastStudiedOn: String? = null
)

@Serializable
data class BadgeResponse(
    val id: String, val code: String, val name: String,
    val description: String? = null, val category: String,
    val xpReward: Int = 0, val earned: Boolean = false,
    val earnedAt: String? = null, val progress: Double? = null
)
/**
 * Study time per day. Mirrors `ActivityResponse`.
 *
 * `days` always covers the whole window, empty days included — the gaps are the honest
 * part of the picture, and a chart that omits them makes a sporadic fortnight look like
 * a habit.
 */
@Serializable
data class ActivityResponse(
    val days: List<DayActivity> = emptyList(),
    val totalMinutes: Int = 0,
    val activeDays: Int = 0,
    /** Averaged over active days, so a rest day does not read as getting worse. */
    val averageMinutesPerActiveDay: Int = 0,
    val byActivity: Map<String, Int> = emptyMap(),
    val bestDay: DayActivity? = null
)

@Serializable
data class DayActivity(
    val date: String = "",
    val minutes: Int = 0,
    val sessions: Int = 0
)

@Serializable
data class StudySessionRequest(
    val startTime: String,
    val endTime: String? = null,
    val topicId: String? = null,
    val focusScore: Int = 0,
    val activity: String? = null
)