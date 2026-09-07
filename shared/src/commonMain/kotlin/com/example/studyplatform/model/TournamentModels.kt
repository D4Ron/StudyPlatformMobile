package com.example.studyplatform.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Competitions. Mirrors the DTOs under `tournaments/dto`.
 *
 * `joined` is server-computed for the calling user and is what decides whether the
 * button says join or compete — the client must not infer it from the participant list,
 * which it does not have.
 */
@Serializable
data class TournamentResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val topic: String? = null,
    val status: String = "",
    val groupId: String? = null,
    val teamsAllowed: Boolean = false,
    val inviteToken: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val questionCount: Int = 0,
    val participantCount: Int = 0,
    val createdByName: String? = null,
    val joined: Boolean = false,
    val createdAt: String? = null
)

/**
 * `payload` stays a raw JSON element: its shape depends on `type`, and a question type
 * the client does not recognise must survive the round trip rather than fail to parse.
 */
@Serializable
data class TournamentQuestionResponse(
    val id: String,
    val type: String = "",
    val payload: JsonElement? = null,
    val points: Int = 0,
    val position: Int = 0,
    val answered: Boolean = false
)

@Serializable
data class LeaderboardEntryResponse(
    val rank: Int = 0,
    val participantId: String? = null,
    val userId: String? = null,
    val displayName: String = "",
    val teamId: String? = null,
    val teamName: String? = null,
    val score: Int = 0,
    val answered: Int = 0
)

@Serializable
data class TeamResponse(
    val id: String,
    val name: String,
    val joinCode: String? = null,
    val captainId: String? = null,
    val memberCount: Int = 0
)

@Serializable
data class SubmissionResponse(
    val id: String,
    val questionId: String? = null,
    val status: String = "",
    val score: Int = 0,
    val feedback: String? = null,
    val submittedAt: String? = null
)

@Serializable
data class TournamentAnswerRequest(
    val questionId: String,
    val answer: String
)

@Serializable
data class CreateTeamRequest(val name: String)
