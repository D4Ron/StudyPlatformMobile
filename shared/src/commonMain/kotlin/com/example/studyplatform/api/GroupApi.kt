package com.example.studyplatform.api

import com.example.studyplatform.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*

object GroupApi {
    suspend fun create(request: CreateGroupRequest): GroupResponse =
        ApiClient.client.post("/api/groups") { setBody(request) }.body()

    suspend fun join(request: JoinGroupRequest): GroupResponse =
        ApiClient.client.post("/api/groups/join") { setBody(request) }.body()

    suspend fun list(): List<GroupResponse> =
        ApiClient.client.get("/api/groups").body()

    suspend fun getById(id: String): GroupResponse =
        ApiClient.client.get("/api/groups/$id").body()

    suspend fun getMembers(groupId: String): List<GroupMemberResponse> =
        ApiClient.client.get("/api/groups/$groupId/members").body()

    suspend fun leave(groupId: String) { ApiClient.client.post("/api/groups/$groupId/leave") }

    suspend fun removeMember(groupId: String, userId: String) {
        ApiClient.client.delete("/api/groups/$groupId/members/$userId")
    }

    suspend fun promoteMember(groupId: String, userId: String) {
        ApiClient.client.post("/api/groups/$groupId/members/$userId/promote")
    }
}
