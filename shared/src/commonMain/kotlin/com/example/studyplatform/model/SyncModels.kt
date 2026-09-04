package com.example.studyplatform.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The sync wire format. These mirror the backend's `core.sync` records exactly —
 * a drift here shows up as a silently dropped change on a phone that was offline,
 * which is the hardest failure on this platform to reproduce.
 */

@Serializable
data class SyncOperationDto(
    /**
     * Generated once, when the change is enqueued, and reused on every retry. This is
     * what makes a retry free: the server recognises the id and replays the original
     * outcome instead of applying the change twice.
     */
    val operationId: String,
    val entityType: String,
    val entityId: String,
    val type: String,          // CREATE | UPDATE | DELETE
    val payload: JsonElement? = null,
    val clientUpdatedAt: String? = null
)

@Serializable
data class SyncRecordDto(
    val entityType: String,
    val entityId: String,
    val payload: JsonElement? = null,
    val updatedAt: String? = null,
    val version: Long = 0,
    /** A tombstone — without it a device cannot tell "deleted elsewhere" from "not yet downloaded". */
    val deleted: Boolean = false
)

@Serializable
data class SyncResultDto(
    val operationId: String,
    val status: String,        // APPLIED | DUPLICATE | CONFLICT | REJECTED
    val record: SyncRecordDto? = null,
    val message: String? = null
)

@Serializable
data class SyncPushRequestDto(val operations: List<SyncOperationDto>)

@Serializable
data class SyncPushResponseDto(
    val results: List<SyncResultDto> = emptyList(),
    val serverTime: String? = null
)

@Serializable
data class SyncPullResponseDto(
    val records: List<SyncRecordDto> = emptyList(),
    val serverTime: String? = null,
    /**
     * True when the server cut the page short and more history remains.
     *
     * Defaults to false so an older server, which does not send the field, is read as
     * "you are caught up" rather than sending the client into an endless loop.
     */
    val hasMore: Boolean = false
)

/** The payload carried for a note, on both the push and the pull side. */
@Serializable
data class NotePayload(
    val title: String,
    val content: String,
    val sharedWithGroup: Boolean = false,
    val groupId: String? = null
)

/**
 * A background task on the server.
 *
 * Generation returns one of these immediately rather than blocking for the length of an
 * AI call — a request held open for ninety seconds does not survive a mobile network.
 */
@Serializable
data class JobResponseDto(
    val id: String,
    val type: String? = null,
    val status: String,        // PENDING | RUNNING | COMPLETED | FAILED
    val title: String? = null,
    val resultId: String? = null,
    val resultType: String? = null,
    val resultPayload: JsonElement? = null,
    val errorMessage: String? = null,
    val createdAt: String? = null,
    val completedAt: String? = null
) {
    val isFinished: Boolean get() = status == "COMPLETED" || status == "FAILED"
}
