package com.example.studyplatform.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.studyplatform.api.AppJson
import com.example.studyplatform.api.SyncTransport
import com.example.studyplatform.db.StudyPlatformDatabase
import com.example.studyplatform.model.*
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject

/**
 * A device, in memory.
 *
 * Each instance is its own SQLite file and its own outbox, so two of them are genuinely
 * two phones — which is what makes "the same note edited on two devices" testable at
 * all.
 */
class FakeDevice(server: FakeServer) {

    val db: StudyPlatformDatabase = StudyPlatformDatabase(
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            .also { StudyPlatformDatabase.Schema.create(it) }
    )

    val notes = NoteRepository(db)
    val study = StudyRepository(db)
    val attempts = AttemptRepository(db)

    val engine = SyncEngine(
        db = db,
        transport = server.transport,
        isAuthenticated = { true }
    )

    fun outboxSize(): Long = db.studyPlatformQueries.countOutbox().executeAsOne()

    fun localNotes() = db.studyPlatformQueries.selectNotes().executeAsList()

    fun noteRow(id: String) = db.studyPlatformQueries.selectNote(id).executeAsOneOrNull()

    fun cursor(entityType: String = "note"): String? =
        db.studyPlatformQueries.selectCursor(entityType).executeAsOneOrNull()?.cursor

    /** Queue entries that have given up, read straight from the table. */
    fun failedCount(): Int =
        db.studyPlatformQueries.selectFailedOutbox(SyncEngine.MAX_ATTEMPTS).executeAsList().size
}

/**
 * A server that behaves the way a real one does over a bad connection.
 *
 * It keeps records, remembers which operation ids it has applied, and can be told to
 * fail, to swallow a reply, or to reject a specific operation — the three things that
 * actually happen and that no amount of testing against a healthy server will produce.
 *
 * The clock matters more than it looks. The real server stamps a record during the
 * transaction and then takes `Instant.now()` for the response, so the cursor it hands
 * back is always *strictly later* than anything it just wrote. A fake that reports the
 * two as equal makes the record invisible to the next `updatedAt > cursor` pull forever —
 * so this one advances between the write and the reply, exactly as the real one does.
 */
class FakeServer {

    private val records = mutableMapOf<String, SyncRecordDto>()
    private val applied = mutableMapOf<String, SyncResultDto>()

    /** Set to make every call throw, as a device with no signal would see. */
    var offline = false

    /**
     * Apply the batch but throw before replying. Models the worst case for
     * idempotency: the work landed and the device has no idea.
     */
    var dropReply = false

    /** Operation ids the server should refuse outright. */
    val reject = mutableSetOf<String>()

    var pushCount = 0
        private set

    /** The server's own clock. Tests move it to model time passing between requests. */
    var clock: Instant = Instant.parse("2026-01-01T09:00:00Z")

    /** The cursor value the last call reported. */
    var lastServerTime: String = clock.toString()
        private set

    fun advance(seconds: Long) {
        clock = clock.plus(kotlin.time.Duration.parse("${seconds}s"))
    }

    /** Stamped on records, then the clock moves on before the reply is sent. */
    private fun stampAndAdvance(): String {
        val stamp = clock.toString()
        clock = clock.plus(kotlin.time.Duration.parse("1s"))
        lastServerTime = clock.toString()
        return stamp
    }

    val transport: SyncTransport = object : SyncTransport {

        override suspend fun push(operations: List<SyncOperationDto>): SyncPushResponseDto {
            if (offline) throw RuntimeException("no connection")
            pushCount++

            val stamp = stampAndAdvance()

            val results = operations.map { op ->
                applied[op.operationId]?.let { return@map it }

                val result = when {
                    op.operationId in reject ->
                        SyncResultDto(op.operationId, "REJECTED", null, "refused by the server")

                    op.type == "DELETE" -> {
                        val tombstone = SyncRecordDto(
                            entityType = op.entityType,
                            entityId = op.entityId,
                            payload = null,
                            updatedAt = stamp,
                            version = (records[op.entityId]?.version ?: 0) + 1,
                            deleted = true
                        )
                        records[op.entityId] = tombstone
                        SyncResultDto(op.operationId, "APPLIED", tombstone, null)
                    }

                    else -> {
                        val existing = records[op.entityId]
                        // Last-write-wins on the client's own timestamp, as the real
                        // handler does; the loser is handed the winner.
                        val serverIsNewer = existing != null &&
                                existing.updatedAt != null &&
                                op.clientUpdatedAt != null &&
                                existing.updatedAt!! > op.clientUpdatedAt!!

                        if (serverIsNewer) {
                            SyncResultDto(op.operationId, "CONFLICT", existing,
                                "the server has a newer version")
                        } else {
                            val stored = SyncRecordDto(
                                entityType = op.entityType,
                                entityId = op.entityId,
                                payload = op.payload,
                                updatedAt = stamp,
                                version = (existing?.version ?: 0) + 1,
                                deleted = false
                            )
                            records[op.entityId] = stored
                            SyncResultDto(op.operationId, "APPLIED", stored, null)
                        }
                    }
                }

                // Recorded before the reply is sent, exactly as the server's
                // operation-id table is — that is what makes a retry safe rather than
                // lucky.
                applied[op.operationId] = when (result.status) {
                    "APPLIED" -> result.copy(status = "DUPLICATE", message = "Already applied.")
                    else -> result
                }
                result
            }

            if (dropReply) throw RuntimeException("connection lost after the server applied it")

            return SyncPushResponseDto(results, lastServerTime)
        }

        override suspend fun pull(since: String?, types: List<String>): SyncPullResponseDto {
            if (offline) throw RuntimeException("no connection")

            val changed = records.values
                .filter { since == null || (it.updatedAt ?: "") > since }
            stampAndAdvance()
            return SyncPullResponseDto(changed, lastServerTime)
        }
    }

    /** Writes a record as though another device had pushed it. */
    fun seed(entityType: String, entityId: String, payload: JsonObject) {
        records[entityId] = SyncRecordDto(
            entityType = entityType,
            entityId = entityId,
            payload = payload,
            updatedAt = stampAndAdvance(),
            version = (records[entityId]?.version ?: 0) + 1,
            deleted = false
        )
    }

    fun record(entityId: String): SyncRecordDto? = records[entityId]

    fun count(entityType: String): Int =
        records.values.count { it.entityType == entityType && !it.deleted }

    fun notePayload(title: String, content: String): JsonObject =
        AppJson.instance.encodeToJsonElement(
            NotePayload.serializer(), NotePayload(title, content)
        ) as JsonObject
}
