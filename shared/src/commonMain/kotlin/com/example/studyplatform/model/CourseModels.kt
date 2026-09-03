package com.example.studyplatform.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The public catalogue — the only content a visitor sees before they have an account.
 *
 * Mirrors `CourseSummaryResponse` / `CourseResponse` on the server.
 */
@Serializable
data class CourseSummary(
    val id: String,
    val title: String,
    val slug: String,
    val summary: String? = null,
    val domain: String? = null,
    val level: String? = null,
    val coverImageUrl: String? = null,
    val chapterCount: Int = 0,
    val authorName: String? = null,
    val publishedAt: String? = null,
    /** Named on the card because the material is someone else's work. */
    val sourceName: String? = null,
    val seed: Boolean = false
)

@Serializable
data class Course(
    val id: String,
    val title: String,
    val slug: String,
    val summary: String? = null,
    val domain: String? = null,
    val level: String? = null,
    val coverImageUrl: String? = null,
    val content: JsonElement? = null,
    val authorName: String? = null,
    val publishedAt: String? = null,
    /**
     * Attribution. Not decoration — CC BY makes naming the source and its licence a
     * condition of using the material at all, so a reader that omits these is in breach.
     */
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val licence: String? = null,
    val seed: Boolean = false
)
