package com.example.studyplatform.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.studyplatform.api.AppJson
import com.example.studyplatform.db.StudyPlatformDatabase
import com.example.studyplatform.model.NotePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Notes, read and written locally.
 *
 * Every method here touches the device database and nothing else. A write returns as
 * soon as the row is stored, and the outbox entry it leaves behind is what eventually
 * reaches the server. Nothing on this path can fail because the network is down, which
 * is the entire point: a student writing notes on a bus with no signal should not be
 * told to try again later.
 */
class NoteRepository(private val db: StudyPlatformDatabase) {

    private val queries = db.studyPlatformQueries

    data class LocalNote(
        val id: String,
        val title: String,
        val content: String,
        val groupId: String?,
        val sharedWithGroup: Boolean,
        val updatedAt: String?,
        /** True while this row still differs from the server, so the UI can mark it. */
        val pending: Boolean
    )

    /** The screen observes this and re-renders on every local change, sync included. */
    fun observe(): Flow<List<LocalNote>> =
        queries.selectNotes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map {
                    LocalNote(
                        id = it.id,
                        title = it.title,
                        content = it.content,
                        groupId = it.group_id,
                        sharedWithGroup = it.shared_with_group == 1L,
                        updatedAt = it.updated_at,
                        pending = it.pending == 1L
                    )
                }
            }

    suspend fun pendingCount(): Long = withContext(Dispatchers.Default) {
        queries.countPendingNotes().executeAsOne()
    }

    /**
     * Creates or edits a note.
     *
     * @param id supplied by the caller for an edit; a new note gets its id here, on the
     *           device, so it has its final identity before it has ever been online.
     */
    suspend fun save(
        id: String = randomUuid(),
        title: String,
        content: String,
        groupId: String? = null,
        sharedWithGroup: Boolean = false
    ): String = withContext(Dispatchers.Default) {
        // Off the caller's thread: SQLDelight writes synchronously wherever it is
        // called, and Compose would be calling it on the main thread.
        val existing = queries.selectNote(id).executeAsOneOrNull()
        val now = Clock.System.now().toString()

        db.transaction {
            queries.upsertNote(
                id = id,
                title = title,
                content = content,
                group_id = groupId,
                shared_with_group = if (sharedWithGroup) 1L else 0L,
                updated_at = now,
                version = existing?.version ?: 0L,
                deleted = 0L,
                pending = 1L
            )
            Outbox.enqueue(
                queries = queries,
                entityType = "note",
                entityId = id,
                type = if (existing == null) "CREATE" else "UPDATE",
                payloadJson = AppJson.instance.encodeToString(
                    NotePayload.serializer(),
                    NotePayload(title, content, sharedWithGroup, groupId)
                ),
                clientTime = now
            )
        }
        id
    }

    /**
     * Deletes a note.
     *
     * The row is tombstoned rather than removed: it has to survive locally until the
     * server has been told, or a pull would simply hand the note back.
     */
    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toString()
        db.transaction {
            queries.markNoteDeleted(updated_at = now, id = id)
            Outbox.enqueue(
                queries = queries,
                entityType = "note",
                entityId = id,
                type = "DELETE",
                payloadJson = null,
                clientTime = now
            )
        }
    }
}
