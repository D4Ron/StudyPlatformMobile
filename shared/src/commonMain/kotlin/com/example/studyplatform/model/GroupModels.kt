package com.example.studyplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupRequest(val name: String, val description: String? = null)

@Serializable
data class JoinGroupRequest(val inviteCode: String)

@Serializable
data class GroupResponse(
    val id: String, val name: String, val description: String? = null,
    val inviteCode: String, val createdByName: String? = null,
    val memberCount: Long = 0, val myRole: String? = null,
    val createdAt: String? = null
)

@Serializable
data class GroupMemberResponse(
    val id: String, val userId: String, val firstName: String,
    val lastName: String, val email: String, val role: String,
    val joinedAt: String? = null
)
