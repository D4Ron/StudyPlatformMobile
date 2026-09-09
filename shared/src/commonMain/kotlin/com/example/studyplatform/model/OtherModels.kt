package com.example.studyplatform.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TopicRequest(val name: String, val specificity: String? = null)
@Serializable
data class TopicResponse(val id: String, val name: String, val specificity: String? = null, val createdAt: String? = null)

@Serializable
data class ExplainConceptRequest(val concept: String, val detailLevel: String)
@Serializable
data class ExplanationResponse(val id: String, val concept: String, val detailLevel: String, val content: JsonElement? = null, val createdAt: String? = null)

@Serializable
data class RecommendationResponse(val id: String, val title: String, val description: String? = null, val reason: String, val relatedTopic: String? = null, val suggestedAction: String, val source: String, val status: String? = null, val createdAt: String? = null)

@Serializable
data class NoteRequest(val title: String, val content: String, val groupId: String? = null, val sharedWithGroup: Boolean = false)
@Serializable
data class NoteUpdateRequest(val title: String? = null, val content: String? = null, val sharedWithGroup: Boolean? = null)
@Serializable
data class NoteResponse(val id: String, val title: String, val content: String, val authorName: String? = null, val groupId: String? = null, val groupName: String? = null, val sharedWithGroup: Boolean = false, val createdAt: String? = null, val updatedAt: String? = null)

@Serializable
data class ChatMessageRequest(val groupId: String, val content: String)
@Serializable
data class ChatMessageResponse(val id: String, val groupId: String, val senderId: String, val senderName: String, val content: String, val sentAt: String? = null)

@Serializable
data class DocumentResponse(val id: String, val filename: String, val contentType: String, val fileSize: Long = 0, val summary: String? = null, val groupId: String? = null, val groupName: String? = null, val uploadedByName: String? = null, val downloadUrl: String? = null, val uploadedAt: String? = null)
@Serializable
data class DocumentUploadResponse(val documentId: String, val filename: String, val contentType: String, val fileSize: Long = 0, val message: String? = null)

@Serializable
data class DriveConnectionStatus(val connected: Boolean = false, val email: String? = null, val connectedAt: String? = null)
@Serializable
data class DriveFileItem(val fileId: String, val name: String, val mimeType: String, val size: Long = 0, val modifiedTime: String? = null, val iconLink: String? = null)
@Serializable
data class DriveImportRequest(val driveFileId: String, val groupId: String? = null)

/**
 * Mirrors `NotificationResponse` on the server.
 *
 * The same shape arrives two ways — `GET /api/notifications/mine` and, once the socket
 * is wired, `/topic/notifications/{userId}`. They are upserted by id and cannot be told
 * apart, so the two must not diverge.
 *
 * `linkPath` is a server-chosen route into the app. It is data from the network, so it
 * is matched against known destinations rather than navigated to verbatim.
 */
@Serializable
data class NotificationResponse(
    val id: String,
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val groupId: String? = null,
    val linkPath: String? = null,
    val read: Boolean = false,
    val createdAt: String? = null
)

/**
 * The signed-in user's profile. Mirrors `UserController.UserProfile`.
 */
@Serializable
data class UserProfile(
    val id: String,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val accountType: String = "STUDENT",
    val educationLevel: String? = null,
    val registrationMode: String? = null,
    val preferenceDomains: List<String> = emptyList(),
    val objectives: String? = null,
    val createdAt: String? = null
)

/**
 * A patch: every field is optional.
 *
 * [AppJson] has `encodeDefaults = true`, so an unset field is sent as an explicit
 * `null` rather than omitted. That is fine because the server treats null as "leave this
 * alone" — but it is the server's rule doing the work, not the wire format's. A field
 * that should be clearable needs a value the server can tell apart from null, which is
 * why `preferenceDomains` clears with an empty list rather than with null.
 */
@Serializable
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val educationLevel: String? = null,
    val registrationMode: String? = null,
    val preferenceDomains: List<String>? = null,
    val objectives: String? = null
)

/**
 * One global-search result. Mirrors `SearchProvider.SearchHit`.
 *
 * `route` is a path the server chose, not a URL — see the server-side note. It is
 * matched against known destinations rather than navigated to verbatim, for the same
 * reason a deep link is.
 */
@Serializable
data class SearchHit(
    val kind: String = "",
    val id: String = "",
    val title: String? = null,
    val snippet: String? = null,
    val route: String? = null,
    val score: Int = 0
)

@Serializable
data class SearchResponse(
    val query: String = "",
    val hits: List<SearchHit> = emptyList(),
    val countsByKind: Map<String, Int> = emptyMap()
)

@Serializable
data class MessageResponse(val message: String)
