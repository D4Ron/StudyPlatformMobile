package com.example.studyplatform.api

import com.example.studyplatform.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.parameter

object TopicApi {
    suspend fun create(request: TopicRequest): TopicResponse =
        ApiClient.client.post("/api/topics") { setBody(request) }.body()
    suspend fun list(): List<TopicResponse> =
        ApiClient.client.get("/api/topics").body()
    suspend fun delete(id: String) { ApiClient.client.delete("/api/topics/$id") }
}

object ExplanationApi {
    suspend fun explain(request: ExplainConceptRequest): ExplanationResponse =
        ApiClient.client.post("/api/explanations") { setBody(request) }.body()
    suspend fun list(): List<ExplanationResponse> =
        ApiClient.client.get("/api/explanations").body()
    suspend fun getById(id: String): ExplanationResponse =
        ApiClient.client.get("/api/explanations/$id").body()
    suspend fun delete(id: String) { ApiClient.client.delete("/api/explanations/$id") }
}

object RecommendationApi {
    suspend fun list(): List<RecommendationResponse> =
        ApiClient.client.get("/api/recommendations").body()
    /**
     * Recomputes recommendations. Returns a job, not a message — this endpoint used to
     * be read as `MessageResponse`, which deserialised into nothing and left the screen
     * with no way to know when the new list was ready.
     */
    suspend fun generate(onStatus: (String) -> Unit = {}): List<RecommendationResponse> {
        val job: JobResponseDto =
            ApiClient.client.post("/api/recommendations/generate").body()
        JobPoller.await(job.id) { onStatus(it.status) }
        return list()
    }
    suspend fun markActed(id: String) { ApiClient.client.post("/api/recommendations/$id/acted") }
}

object NoteApi {
    suspend fun create(request: NoteRequest): NoteResponse =
        ApiClient.client.post("/api/notes") { setBody(request) }.body()
    suspend fun update(id: String, request: NoteUpdateRequest): NoteResponse =
        ApiClient.client.patch("/api/notes/$id") { setBody(request) }.body()
    suspend fun listMine(): List<NoteResponse> =
        ApiClient.client.get("/api/notes/mine").body()
    suspend fun listGroup(groupId: String): List<NoteResponse> =
        ApiClient.client.get("/api/notes/group/$groupId").body()
    suspend fun getById(id: String): NoteResponse =
        ApiClient.client.get("/api/notes/$id").body()
    suspend fun delete(id: String) { ApiClient.client.delete("/api/notes/$id") }
}

object ChatApi {
    suspend fun send(request: ChatMessageRequest): ChatMessageResponse =
        ApiClient.client.post("/api/chat") { setBody(request) }.body()
    suspend fun history(groupId: String, limit: Int = 50): List<ChatMessageResponse> =
        ApiClient.client.get("/api/chat/$groupId") { parameter("limit", limit) }.body()
}

object DriveApi {
    suspend fun getConnectUrl(): Map<String, String> =
        ApiClient.client.get("/api/drive/connect").body()
    suspend fun getStatus(): DriveConnectionStatus =
        ApiClient.client.get("/api/drive/status").body()
    suspend fun disconnect(): MessageResponse =
        ApiClient.client.delete("/api/drive/disconnect").body()
    suspend fun listFiles(query: String? = null): List<DriveFileItem> =
        ApiClient.client.get("/api/drive/files") { if (query != null) parameter("query", query) }.body()
    suspend fun import(request: DriveImportRequest): DocumentUploadResponse =
        ApiClient.client.post("/api/drive/import") { setBody(request) }.body()
}

object DocumentApi {
    suspend fun listMine(): List<DocumentResponse> =
        ApiClient.client.get("/api/documents/mine").body()
    suspend fun listGroup(groupId: String): List<DocumentResponse> =
        ApiClient.client.get("/api/documents/group/$groupId").body()
    suspend fun getById(id: String): DocumentResponse =
        ApiClient.client.get("/api/documents/$id").body()
    suspend fun delete(id: String) { ApiClient.client.delete("/api/documents/$id") }
}
