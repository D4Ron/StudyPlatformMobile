package com.example.studyplatform.api

import com.example.studyplatform.model.JobResponseDto
import kotlinx.coroutines.delay

/**
 * Waits for a background job to finish.
 *
 * Generation endpoints return a job immediately instead of the finished artifact,
 * because an AI call takes tens of seconds and a request held open that long does not
 * survive a mobile network. The web follows the job over a WebSocket; this client has no
 * socket, so it polls.
 *
 * The interval widens as the wait goes on: a guide is usually ready in well under a
 * minute, and a phone on a metered connection should not be asking every second for the
 * whole of a slow one.
 */
/**
 * The job itself failed or never finished — distinct from a network error, and carrying
 * a message worth showing, so callers can rethrow it instead of flattening it into a
 * generic "could not generate" that tells the user nothing.
 */
class JobFailedException(message: String) : Exception(message)

object JobPoller {

    private const val FIRST_DELAY_MS = 1_500L
    private const val MAX_DELAY_MS = 6_000L
    private const val TIMEOUT_MS = 180_000L

    /**
     * @param onProgress called with each status seen, so a screen can say what is
     *                   happening rather than showing an unexplained spinner
     * @return the finished job — the caller reads `resultId` and fetches the artifact
     * @throws Exception if the job fails or takes longer than three minutes
     */
    suspend fun await(
        jobId: String,
        onProgress: (JobResponseDto) -> Unit = {}
    ): JobResponseDto {
        var waited = 0L
        var interval = FIRST_DELAY_MS

        while (waited < TIMEOUT_MS) {
            delay(interval)
            waited += interval
            interval = minOf((interval * 1.4).toLong(), MAX_DELAY_MS)

            val job = try {
                JobApi.get(jobId)
            } catch (e: Exception) {
                // A dropped poll is not a failed job. The work continues on the server,
                // so keep asking until the overall timeout.
                println("Job poll failed (will retry): ${e.message}")
                continue
            }

            onProgress(job)

            if (job.status == "COMPLETED") return job
            if (job.status == "FAILED") {
                throw JobFailedException(job.errorMessage ?: "That task failed. Please try again.")
            }
        }

        // The job is still running server-side; it will appear in the list when it lands.
        throw JobFailedException("This is taking longer than usual. It will show up in your library once it finishes.")
    }
}
