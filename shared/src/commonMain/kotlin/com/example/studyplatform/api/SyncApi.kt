package com.example.studyplatform.api

import com.example.studyplatform.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * How the sync engine reaches the server.
 *
 * An interface rather than a direct call so the engine can be driven against a fake in
 * tests. Every interesting sync behaviour — a lost reply, a conflict, a batch that half
 * fails — is defined by what the server said, and there is no way to exercise those
 * against a real socket.
 */
interface SyncTransport {
    suspend fun push(operations: List<SyncOperationDto>): SyncPushResponseDto
    suspend fun pull(since: String?, types: List<String>): SyncPullResponseDto
}

object SyncApi : SyncTransport {

    /** Drains the outbox. Always 200 — the outcome is reported per operation. */
    override suspend fun push(operations: List<SyncOperationDto>): SyncPushResponseDto =
        ApiClient.client.post("/api/sync/push") {
            setBody(SyncPushRequestDto(operations))
        }.body()

    /**
     * Downloads what changed elsewhere.
     *
     * @param since the cursor from the last successful pull, in server time. Null on a
     *              first sync, which returns everything.
     */
    override suspend fun pull(since: String?, types: List<String>): SyncPullResponseDto =
        ApiClient.client.get("/api/sync/pull") {
            if (since != null) parameter("since", since)
            types.forEach { parameter("types", it) }
        }.body()
}

object JobApi {

    /** One job by id — what generation polls while it waits. */
    suspend fun get(jobId: String): JobResponseDto =
        ApiClient.client.get("/api/jobs/$jobId").body()

    suspend fun mine(): List<JobResponseDto> =
        try {
            ApiClient.client.get("/api/jobs/mine").body()
        } catch (e: Exception) {
            println("Job list error: ${e.message}")
            emptyList()
        }
}
