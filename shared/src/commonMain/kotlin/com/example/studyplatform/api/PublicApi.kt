package com.example.studyplatform.api

import com.example.studyplatform.model.Course
import com.example.studyplatform.model.CourseSummary
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * The unauthenticated surface, backing guest mode.
 *
 * These are the only calls that work without an account. `ApiClient` attaches a bearer
 * token when one exists and simply omits it otherwise, so the same client serves a guest
 * and a signed-in reader without a second configuration.
 */
object PublicApi {

    suspend fun courses(domain: String? = null, limit: Int = 24): List<CourseSummary> =
        ApiClient.client.get("/api/public/courses") {
            if (domain != null) parameter("domain", domain)
            parameter("limit", limit)
        }.body()

    /** By slug, not id — the same URL the web reader uses. */
    suspend fun course(slug: String): Course =
        ApiClient.client.get("/api/public/courses/$slug").body()
}
