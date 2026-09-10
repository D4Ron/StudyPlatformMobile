package com.example.studyplatform.data

import com.example.studyplatform.api.*
import com.example.studyplatform.model.*

/**
 * Every screen that only reads.
 *
 * Each method tries the network first and falls back to the last copy this device saw.
 * The result says which it got, so a screen can tell the student they are looking at
 * something downloaded earlier rather than presenting stale data as current.
 *
 * Network-first rather than cache-first: these records change on the server (a guide is
 * generated, a badge is earned) and the device has no way to know that it has fallen
 * behind. Showing the cache first would mean showing yesterday's library to someone who
 * has signal, which is the wrong trade for data this small.
 */
class LibraryRepository(private val cache: CacheStore) {

    // ── Public catalogue (guest mode) ────────────────────────────────
    //
    // Cached like everything else, and deliberately so: a visitor deciding whether this
    // app is worth an account is the least likely person to be on a good connection,
    // and a catalogue that goes blank on the bus is the version of the product they
    // would remember.

    suspend fun publicCourses(domain: String? = null): Offline<List<CourseSummary>> =
        cache.list(
            key = if (domain == null) "public-courses" else "public-courses:$domain",
            entityType = "course-summary",
            serializer = CourseSummary.serializer(),
            idOf = { it.slug },
            fetch = { PublicApi.courses(domain) }
        )

    suspend fun publicCourse(slug: String): Offline<Course?> =
        cache.one("course", slug, Course.serializer()) { PublicApi.course(slug) }

    // ── Guides ───────────────────────────────────────────────────────

    suspend fun guides(): Offline<List<GuideListItem>> =
        cache.list(
            key = "guides",
            entityType = "guide-item",
            serializer = GuideListItem.serializer(),
            idOf = { it.id },
            fetch = { GuideApi.list() }
        )

    /**
     * A whole guide, including its content.
     *
     * This is the single most important thing to have offline: a study guide is
     * something you sit and read for half an hour, and the reading is exactly when a
     * student is least likely to be somewhere with signal.
     */
    suspend fun guide(id: String): Offline<GuideResponse?> =
        cache.one("guide", id, GuideResponse.serializer()) { GuideApi.getById(id) }

    // ── Quizzes ──────────────────────────────────────────────────────

    suspend fun quizzes(): Offline<List<QuizListItem>> =
        cache.list(
            key = "quizzes",
            entityType = "quiz-item",
            serializer = QuizListItem.serializer(),
            idOf = { it.id },
            fetch = { QuizApi.list() }
        )

    /** The full quiz, questions included — which is what makes it answerable offline. */
    suspend fun quiz(id: String): Offline<QuizResponse?> =
        cache.one("quiz", id, QuizResponse.serializer()) { QuizApi.getById(id) }

    suspend fun myAttempts(): Offline<List<QuizAttemptResponse>> =
        cache.list(
            key = "attempts",
            entityType = "attempt",
            serializer = QuizAttemptResponse.serializer(),
            idOf = { it.id },
            fetch = { QuizApi.getMyAttempts() }
        )

    // ── Explanations ─────────────────────────────────────────────────

    suspend fun explanations(): Offline<List<ExplanationResponse>> =
        cache.list(
            key = "explanations",
            entityType = "explanation",
            serializer = ExplanationResponse.serializer(),
            idOf = { it.id },
            fetch = { ExplanationApi.list() }
        )

    // ── Progress ─────────────────────────────────────────────────────

    suspend fun dashboard(): Offline<DashboardStats?> =
        cache.singleton("dashboard", DashboardStats.serializer()) { StatsApi.getDashboard() }

    suspend fun level(): Offline<LevelResponse?> =
        cache.singleton("level", LevelResponse.serializer()) { StatsApi.getLevel() }

    /**
     * Cached, so the streak still shows on a dead connection.
     *
     * A cached streak can be a day stale, which is why the screen reads `studiedToday`
     * rather than inferring it — a stale "not yet today" is a nudge, while a stale
     * count presented as current would be a lie about work the student did offline.
     */
    suspend fun streak(): Offline<StreakResponse?> =
        cache.singleton("streak", StreakResponse.serializer()) { StatsApi.getStreak() }

    /**
     * Study minutes per day.
     *
     * Cached like the rest of the dashboard, and the window is part of the key: a
     * 7-day chart and a 30-day chart are different answers, and serving one from the
     * other's cache would silently show the wrong period.
     */
    suspend fun activity(days: Int = 30): Offline<ActivityResponse?> =
        cache.singleton("activity:$days", ActivityResponse.serializer()) {
            StatsApi.getActivity(days)
        }

    suspend fun badges(): Offline<List<BadgeResponse>> =
        cache.list(
            key = "badges",
            entityType = "badge",
            serializer = BadgeResponse.serializer(),
            idOf = { it.id },
            fetch = { StatsApi.getBadges() }
        )

    suspend fun recommendations(): Offline<List<RecommendationResponse>> =
        cache.list(
            key = "recommendations",
            entityType = "recommendation",
            serializer = RecommendationResponse.serializer(),
            idOf = { it.id },
            fetch = { RecommendationApi.list() }
        )

    // ── Groups ───────────────────────────────────────────────────────

    suspend fun groups(): Offline<List<GroupResponse>> =
        cache.list(
            key = "groups",
            entityType = "group",
            serializer = GroupResponse.serializer(),
            idOf = { it.id },
            fetch = { GroupApi.list() }
        )

    suspend fun members(groupId: String): Offline<List<GroupMemberResponse>> =
        cache.list(
            key = "members:$groupId",
            entityType = "member",
            serializer = GroupMemberResponse.serializer(),
            idOf = { it.id },
            fetch = { GroupApi.getMembers(groupId) }
        )

    suspend fun group(id: String): Offline<GroupResponse?> =
        cache.one("group", id, GroupResponse.serializer()) { GroupApi.getById(id) }

    /**
     * Chat history.
     *
     * Readable offline, but not writable: a message posted into a group that nobody
     * else can see yet is a promise the app cannot keep, and there is no useful
     * conflict resolution for a conversation. Sending stays online-only and says so.
     */
    suspend fun chat(groupId: String): Offline<List<ChatMessageResponse>> =
        cache.list(
            key = "chat:$groupId",
            entityType = "chat",
            serializer = ChatMessageResponse.serializer(),
            idOf = { it.id },
            fetch = { ChatApi.history(groupId) }
        )

    // ── Documents ────────────────────────────────────────────────────

    /**
     * The document list — metadata only.
     *
     * The files themselves are not cached: they are arbitrarily large, and filling a
     * student's storage with PDFs they did not ask to download is the opposite of
     * helping someone on a cheap phone.
     */
    suspend fun documents(): Offline<List<DocumentResponse>> =
        cache.list(
            key = "documents",
            entityType = "document",
            serializer = DocumentResponse.serializer(),
            idOf = { it.id },
            fetch = { DocumentApi.listMine() }
        )

    // ── Notifications ────────────────────────────────────────────────

    /**
     * Cached so the list still opens on a dead connection, showing what was true at the
     * last sync. Marking one read is deliberately not queued for later: an unread badge
     * that clears itself while offline and then reappears is worse than one that waits.
     */
    suspend fun notifications(): Offline<List<NotificationResponse>> =
        cache.list(
            key = "notifications",
            entityType = "notification",
            serializer = NotificationResponse.serializer(),
            idOf = { it.id },
            fetch = { NotificationApi.listMine() }
        )
}
