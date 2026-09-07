package com.example.studyplatform.api

import com.example.studyplatform.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Competitions.
 *
 * Everything here is online-only, deliberately. A tournament is a live contest against
 * other people: a locally queued answer that uploads an hour later would either be
 * scored against a leaderboard that has moved on, or would have to be silently dropped.
 * Both are worse than telling the student they need a connection to compete.
 */
object TournamentApi {

    /** Tournaments the caller can see — public ones plus those in their groups. */
    suspend fun listVisible(): List<TournamentResponse> =
        ApiClient.client.get("/api/tournaments").body()

    suspend fun listMine(): List<TournamentResponse> =
        ApiClient.client.get("/api/tournaments/mine").body()

    suspend fun getById(id: String): TournamentResponse =
        ApiClient.client.get("/api/tournaments/$id").body()

    suspend fun getByInvite(token: String): TournamentResponse =
        ApiClient.client.get("/api/tournaments/invite/$token").body()

    suspend fun joinSolo(id: String) {
        ApiClient.client.post("/api/tournaments/$id/join")
    }

    suspend fun createTeam(id: String, name: String): TeamResponse =
        ApiClient.client.post("/api/tournaments/$id/teams") {
            setBody(CreateTeamRequest(name))
        }.body()

    suspend fun joinTeam(code: String): TeamResponse =
        ApiClient.client.post("/api/tournaments/teams/join/$code").body()

    /** Correct answers are stripped server-side, so this is safe to hold in memory. */
    suspend fun questions(id: String): List<TournamentQuestionResponse> =
        ApiClient.client.get("/api/tournaments/$id/questions").body()

    suspend fun submit(id: String, questionId: String, answer: String): SubmissionResponse =
        ApiClient.client.post("/api/tournaments/$id/submit") {
            setBody(TournamentAnswerRequest(questionId, answer))
        }.body()

    suspend fun leaderboard(id: String): List<LeaderboardEntryResponse> =
        ApiClient.client.get("/api/tournaments/$id/leaderboard").body()
}
