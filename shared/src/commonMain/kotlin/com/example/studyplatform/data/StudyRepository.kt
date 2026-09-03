package com.example.studyplatform.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.studyplatform.api.AppJson
import com.example.studyplatform.db.StudyPlatformDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/** What travels for a topic. Mirrors `TopicSyncHandler` on the server. */
@Serializable
data class TopicPayload(val name: String, val specificity: String? = null)

/** What travels for a study session. Mirrors `StudySessionSyncHandler`. */
@Serializable
data class StudySessionPayload(
    val startTime: String,
    val endTime: String? = null,
    val topicId: String? = null,
    val focusScore: Int = 0,
    val activity: String? = null
)

/** What travels for a quiz attempt. Mirrors `QuizAttemptSyncHandler`. */
@Serializable
data class AttemptPayload(
    val quizId: String,
    val answers: JsonArray,
    val timeTakenSeconds: Int = 0,
    /** When the student actually took it, not when it reached the server. */
    val takenAt: String
)

/**
 * Topics and study sessions — the two things besides notes that a student creates while
 * they are working, which is to say while they are least likely to have signal.
 *
 * Same shape as [NoteRepository]: write locally, enqueue, return. Nothing here can fail
 * because the network is down.
 */
class StudyRepository(private val db: StudyPlatformDatabase) {

    private val queries = db.studyPlatformQueries

    data class LocalTopic(
        val id: String,
        val name: String,
        val specificity: String?,
        val pending: Boolean
    )

    data class LocalSession(
        val id: String,
        val topicId: String?,
        val startTime: String,
        val endTime: String?,
        val focusScore: Int,
        val activity: String?,
        val pending: Boolean
    )

    // ── Topics ───────────────────────────────────────────────────────

    fun observeTopics(): Flow<List<LocalTopic>> =
        queries.selectTopics()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map {
                    LocalTopic(it.id, it.name, it.specificity, it.pending == 1L)
                }
            }

    suspend fun saveTopic(
        id: String = randomUuid(),
        name: String,
        specificity: String? = null
    ): String = withContext(Dispatchers.Default) {
        val existing = queries.selectTopic(id).executeAsOneOrNull()
        val now = Clock.System.now().toString()

        db.transaction {
            queries.upsertTopic(
                id = id,
                name = name,
                specificity = specificity,
                updated_at = now,
                version = existing?.version ?: 0L,
                deleted = 0L,
                pending = 1L
            )
            Outbox.enqueue(
                queries = queries,
                entityType = "topic",
                entityId = id,
                type = if (existing == null) "CREATE" else "UPDATE",
                payloadJson = AppJson.instance.encodeToString(
                    TopicPayload.serializer(), TopicPayload(name, specificity)
                ),
                clientTime = now
            )
        }
        id
    }

    suspend fun deleteTopic(id: String) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toString()
        db.transaction {
            queries.markTopicDeleted(updated_at = now, id = id)
            Outbox.enqueue(queries, "topic", id, "DELETE", null, now)
        }
    }

    // ── Study sessions ───────────────────────────────────────────────

    fun observeSessions(limit: Long = 50): Flow<List<LocalSession>> =
        queries.selectSessions(limit)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map {
                    LocalSession(
                        id = it.id,
                        topicId = it.topic_id,
                        startTime = it.start_time,
                        endTime = it.end_time,
                        focusScore = it.focus_score.toInt(),
                        activity = it.activity,
                        pending = it.pending == 1L
                    )
                }
            }

    suspend fun saveSession(
        id: String = randomUuid(),
        startTime: String,
        endTime: String? = null,
        topicId: String? = null,
        focusScore: Int = 0,
        activity: String? = null
    ): String = withContext(Dispatchers.Default) {
        val existing = queries.selectSession(id).executeAsOneOrNull()
        val now = Clock.System.now().toString()

        db.transaction {
            queries.upsertSession(
                id = id,
                topic_id = topicId,
                start_time = startTime,
                end_time = endTime,
                focus_score = focusScore.toLong(),
                activity = activity,
                updated_at = now,
                version = existing?.version ?: 0L,
                deleted = 0L,
                pending = 1L
            )
            Outbox.enqueue(
                queries = queries,
                entityType = "study-session",
                entityId = id,
                type = if (existing == null) "CREATE" else "UPDATE",
                payloadJson = AppJson.instance.encodeToString(
                    StudySessionPayload.serializer(),
                    StudySessionPayload(startTime, endTime, topicId, focusScore, activity)
                ),
                clientTime = now
            )
        }
        id
    }
}

/**
 * Quiz attempts taken without a connection.
 *
 * The score is computed here because the quiz payload carries the correct answer with
 * every question — the take screen already reads it to mark answers as you go. The
 * server re-scores on upload and its result is the one that counts, including the XP;
 * this exists so the student sees their result the moment they finish rather than being
 * told to come back when they have signal.
 *
 * Append-only. An attempt records something that happened, so there is no update, no
 * delete, and no conflict to resolve — only the question of whether the server has it
 * yet.
 */
class AttemptRepository(private val db: StudyPlatformDatabase) {

    private val queries = db.studyPlatformQueries

    data class LocalAttempt(
        val id: String,
        val quizId: String,
        val quizTitle: String?,
        val correctCount: Int,
        val totalQuestions: Int,
        val timeTakenSeconds: Int,
        val takenAt: String
    ) {
        val percentageScore: Double
            get() = if (totalQuestions == 0) 0.0
            else correctCount * 100.0 / totalQuestions
    }

    fun observePending(): Flow<List<LocalAttempt>> =
        queries.selectPendingAttempts()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map {
                    LocalAttempt(
                        id = it.id,
                        quizId = it.quiz_id,
                        quizTitle = it.quiz_title,
                        correctCount = it.correct_count.toInt(),
                        totalQuestions = it.total_questions.toInt(),
                        timeTakenSeconds = it.time_taken_seconds.toInt(),
                        takenAt = it.taken_at
                    )
                }
            }

    suspend fun pendingCount(): Long = withContext(Dispatchers.Default) {
        queries.countPendingAttempts().executeAsOne()
    }

    /**
     * Records a finished attempt and queues it for upload.
     *
     * @param answers the raw submission array, stored verbatim so the server scores the
     *                same answers the student actually gave rather than our reading of
     *                them
     */
    suspend fun record(
        quizId: String,
        quizTitle: String?,
        answers: JsonArray,
        correctCount: Int,
        totalQuestions: Int,
        timeTakenSeconds: Int
    ): LocalAttempt = withContext(Dispatchers.Default) {
        val id = randomUuid()
        val now = Clock.System.now().toString()
        val answersJson = answers.toString()

        db.transaction {
            queries.insertPendingAttempt(
                id = id,
                quiz_id = quizId,
                quiz_title = quizTitle,
                answers = answersJson,
                correct_count = correctCount.toLong(),
                total_questions = totalQuestions.toLong(),
                time_taken_seconds = timeTakenSeconds.toLong(),
                taken_at = now
            )
            Outbox.enqueue(
                queries = queries,
                entityType = "quiz-attempt",
                entityId = id,
                type = "CREATE",
                payloadJson = AppJson.instance.encodeToString(
                    AttemptPayload.serializer(),
                    AttemptPayload(quizId, answers, timeTakenSeconds, now)
                ),
                clientTime = now
            )
        }

        LocalAttempt(id, quizId, quizTitle, correctCount, totalQuestions, timeTakenSeconds, now)
    }
}
