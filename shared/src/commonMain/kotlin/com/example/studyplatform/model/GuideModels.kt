package com.example.studyplatform.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GenerateGuideRequest(
    val topic: String, val expertiseLevel: String,
    val specificConcept: String? = null,
    val documentId: String? = null, val topicId: String? = null
)

@Serializable
data class GuideResponse(
    val id: String, val title: String, val topic: String? = null,
    val specificConcept: String? = null, val expertiseLevel: String,
    val broadOverview: Boolean = false, val content: JsonElement? = null,
    val totalEstimatedMinutes: Int = 0, val documentId: String? = null,
    val createdAt: String? = null
)

@Serializable
data class GuideListItem(
    val id: String, val title: String, val topic: String? = null,
    val expertiseLevel: String, val totalEstimatedMinutes: Int = 0,
    val moduleCount: Int = 0, val createdAt: String? = null
)
