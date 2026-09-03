package com.example.studyplatform.data

import com.example.studyplatform.api.ApiClient
import com.example.studyplatform.api.AppJson
import com.example.studyplatform.api.SyncApi
import com.example.studyplatform.api.SyncTransport
import com.example.studyplatform.db.StudyPlatformDatabase
import com.example.studyplatform.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject

/**
 * Reconciles the device with the server.
 *
 * Push first, then pull. The order matters: pulling first would overwrite a local edit
 * with the server's older copy and the user's work would disappear without anything
 * having gone wrong.
 *
 * Nothing here throws on a network failure. A sync that cannot reach the server is not
 * an error — it is the normal state on this platform, and the next attempt will carry
 * the same queue.
 */
class SyncEngine(
    private val db: StudyPlatformDatabase,
    private val entityTypes: List<String> =
        listOf("note", "topic", "study-session", "quiz-attempt"),
    private val transport: SyncTransport = SyncApi,
    /**
     * Whether there is a session to sync under. Injected so tests can drive the engine
     * without a token store.
     */
    private val isAuthenticated: () -> Boolean = { ApiClient.isLoggedIn() }
) {

    private val queries = db.studyPlatformQueries

    companion object {
        private const val BATCH_SIZE = 50L

        /**
         * After this many failures an entry stops being retried silently and is
         * surfaced instead. A change the server will never accept would otherwise loop
         * forever, burning data on a connection that costs money.
         */
        const val MAX_ATTEMPTS = 5L
    }

    data class Outcome(
        val pushed: Int = 0,
        val conflicts: Int = 0,
        val rejected: Int = 0,
        val pulled: Int = 0,
        val failed: Boolean = false,
        val message: String? = null
    )

    suspend fun sync(): Outcome = withContext(Dispatchers.Default) {
        // Never on the caller's thread: this does synchronous SQLite work between the
        // network calls, and the UI calls it directly.
        if (!isAuthenticated()) return@withContext Outcome()

        try {
            val push = push()
            val pulled = pull()
            push.copy(pulled = pulled)
        } catch (e: Exception) {
            println("Sync could not complete: ${e.message}")
            Outcome(failed = true, message = e.message)
        }
    }

    // ── Push ─────────────────────────────────────────────────────────────

    private suspend fun push(): Outcome {
        val batch = queries.selectOutboxBatch(BATCH_SIZE).executeAsList()
            .filter { it.attempts < MAX_ATTEMPTS }
        if (batch.isEmpty()) return Outcome()

        val operations = batch.map {
            SyncOperationDto(
                operationId = it.operation_id,
                entityType = it.entity_type,
                entityId = it.entity_id,
                type = it.operation_type,
                payload = it.payload?.let { raw -> AppJson.instance.parseToJsonElement(raw) },
                clientUpdatedAt = it.client_time
            )
        }

        val response = try {
            transport.push(operations)
        } catch (e: Exception) {
            // The batch stays queued. Record the failure so an entry that keeps failing
            // eventually stops being retried rather than looping forever.
            batch.forEach {
                queries.recordOutboxFailure(
                    last_error = e.message ?: "network",
                    operation_id = it.operation_id
                )
            }
            return Outcome(failed = true, message = e.message)
        }

        var pushed = 0
        var conflicts = 0
        var rejected = 0
        val byId = batch.associateBy { it.operation_id }

        db.transaction {
            response.results.forEach { result ->
                val entry = byId[result.operationId] ?: return@forEach
                when (result.status) {
                    // DUPLICATE means an earlier attempt did land and we lost the reply.
                    // It is a success, not an error — that is what operation ids buy us.
                    "APPLIED", "DUPLICATE" -> {
                        pushed++
                        queries.deleteOutboxEntry(result.operationId)
                        result.record?.let { applyRecord(it) }
                    }
                    "CONFLICT" -> {
                        conflicts++
                        queries.deleteOutboxEntry(result.operationId)
                        // The server's version won; take it so the user sees what
                        // survived rather than a local copy that no longer exists.
                        result.record?.let { applyRecord(it) }
                    }
                    "REJECTED" -> {
                        rejected++
                        queries.deleteOutboxEntry(result.operationId)
                        println("Sync rejected ${entry.entity_type} ${entry.entity_id}: ${result.message}")
                    }
                }
            }
        }

        return Outcome(pushed = pushed, conflicts = conflicts, rejected = rejected)
    }

    // ── Pull ─────────────────────────────────────────────────────────────

    private suspend fun pull(): Int {
        // One cursor covers the batch: the types are pulled together, and the server
        // clock they share is what makes the next window contiguous.
        val cursor = queries.selectCursor(entityTypes.first()).executeAsOneOrNull()?.cursor
        val response = transport.pull(cursor, entityTypes)

        db.transaction {
            // A row with unsent local edits is left alone. Push runs first, so a row is
            // only still pending because its upload failed — and overwriting it here
            // would delete work the user did while offline, which is the one outcome
            // this whole design exists to prevent.
            response.records.forEach { applyRecord(it, force = false) }
            response.serverTime?.let { server ->
                entityTypes.forEach { queries.setCursor(entity_type = it, cursor = server) }
            }
        }
        return response.records.size
    }

    // ── Applying a server record ─────────────────────────────────────────

    /**
     * Writes the server's copy over the local one.
     *
     * @param force true when this record is the confirmed outcome of one of our own
     *              operations — applied, duplicated, or lost to a conflict. In that case
     *              the server's copy is authoritative even over a pending local row,
     *              because the pending row is the very change being answered.
     */
    private fun applyRecord(record: SyncRecordDto, force: Boolean = true) {
        when (record.entityType) {
            "note" -> applyNote(record, force)
            "topic" -> applyTopic(record, force)
            "study-session" -> applySession(record, force)
            // An attempt is a fact the server confirmed. There is nothing to reconcile:
            // the local copy existed only so the student could see their score before
            // the upload, and once it has landed the server's own list supersedes it.
            "quiz-attempt" -> queries.deletePendingAttempt(record.entityId)
            else -> println("Ignoring unknown record type '${record.entityType}'")
        }
    }

    private fun applyNote(record: SyncRecordDto, force: Boolean) {
        if (!force && queries.selectNote(record.entityId).executeAsOneOrNull()?.pending == 1L) return

        if (record.deleted) {
            // The tombstone has been round-tripped, so the local row can finally go.
            queries.deleteNoteRow(record.entityId)
            return
        }

        val payload = record.payload?.jsonObject ?: return
        val note = AppJson.instance.decodeFromJsonElement(NotePayload.serializer(), payload)

        queries.upsertNote(
            id = record.entityId,
            title = note.title,
            content = note.content,
            group_id = note.groupId,
            shared_with_group = if (note.sharedWithGroup) 1L else 0L,
            updated_at = record.updatedAt,
            version = record.version,
            deleted = 0L,
            pending = 0L
        )
    }

    private fun applyTopic(record: SyncRecordDto, force: Boolean) {
        if (!force && queries.selectTopic(record.entityId).executeAsOneOrNull()?.pending == 1L) return

        if (record.deleted) {
            queries.deleteTopicRow(record.entityId)
            return
        }

        val payload = record.payload?.jsonObject ?: return
        val topic = AppJson.instance.decodeFromJsonElement(TopicPayload.serializer(), payload)

        queries.upsertTopic(
            id = record.entityId,
            name = topic.name,
            specificity = topic.specificity,
            updated_at = record.updatedAt,
            version = record.version,
            deleted = 0L,
            pending = 0L
        )
    }

    private fun applySession(record: SyncRecordDto, force: Boolean) {
        if (!force && queries.selectSession(record.entityId).executeAsOneOrNull()?.pending == 1L) return

        if (record.deleted) {
            queries.deleteSessionRow(record.entityId)
            return
        }

        val payload = record.payload?.jsonObject ?: return
        val session = AppJson.instance
            .decodeFromJsonElement(StudySessionPayload.serializer(), payload)

        queries.upsertSession(
            id = record.entityId,
            topic_id = session.topicId,
            start_time = session.startTime,
            end_time = session.endTime,
            focus_score = session.focusScore.toLong(),
            activity = session.activity,
            updated_at = record.updatedAt,
            version = record.version,
            deleted = 0L,
            pending = 0L
        )
    }

    /** Queue entries that have given up. Shown to the user rather than retried. */
    suspend fun failedOperations(): Int = withContext(Dispatchers.Default) {
        queries.selectFailedOutbox(MAX_ATTEMPTS).executeAsList().size
    }
}
