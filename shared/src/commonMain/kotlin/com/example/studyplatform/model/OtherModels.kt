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

@Serializable
data class MessageResponse(val message: String)
