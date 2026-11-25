package com.example.studyplatform.api

import com.example.studyplatform.i18n.Msg

import com.example.studyplatform.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*

object GuideApi {
    /**
     * Starts generation and waits for it.
     *
     * The endpoint returns a job rather than the guide — the LLM call takes tens of
     * seconds, and a request held open that long does not survive a mobile network. We
     * poll until the job completes, then fetch the guide it produced.
     *
     * @param onStatus surfaces the job's state so the screen can say what is happening
     */
    suspend fun generate(
        request: GenerateGuideRequest,
        onStatus: (String) -> Unit = {}
    ): GuideResponse {
        try {
            println("Generating guide: topic=${request.topic}, level=${request.expertiseLevel}")
            val job: JobResponseDto =
                ApiClient.client.post("/api/guides/generate") { setBody(request) }.body()

            val finished = JobPoller.await(job.id) { onStatus(it.status) }
            val guideId = finished.resultId
                ?: throw Exception(Msg.t("The guide was generated but could not be located.", "Le guide a été généré mais reste introuvable."))

            val guide = getById(guideId)
            println("Guide generated: ${guide.id} — ${guide.title}")
            return guide
        } catch (e: JobFailedException) {
            // The server explained what went wrong; don't replace that with a generic line.
            throw e
        } catch (e: ApiException) {
            println("Guide generation API error [${e.statusCode}]: ${e.rawBody}")
            when (e.statusCode) {
                502, 503, 504 -> throw Exception(Msg.t("The server is waking up. Please wait 30 seconds and try again.", "Le serveur se réveille. Patientez 30 secondes puis réessayez."))
                else -> throw Exception(Msg.t("Could not generate the guide. Please try again.", "Impossible de générer le guide. Réessayez."))
            }
        } catch (e: Exception) {
            if (e.message?.contains("timed out") == true || e.message?.contains("Timeout") == true) {
                println("Guide generation timeout: ${e.message}")
                throw Exception(Msg.t("The request timed out. The server might be starting up — please try again in a moment.", "Délai dépassé. Le serveur démarre peut-être — réessayez dans un instant."))
            }
            println("Guide generation error: ${e.message}")
            throw Exception(Msg.t("Could not generate the guide. Please check your connection and try again.", "Impossible de générer le guide. Vérifiez votre connexion et réessayez."))
        }
    }

    suspend fun list(): List<GuideListItem> {
        try {
            return ApiClient.client.get("/api/guides").body()
        } catch (e: Exception) {
            println("Guide list error: ${e.message}")
            return emptyList()
        }
    }

    suspend fun getById(id: String): GuideResponse {
        try {
            return ApiClient.client.get("/api/guides/$id").body()
        } catch (e: Exception) {
            println("Guide fetch error: ${e.message}")
            throw Exception(Msg.t("Could not load the guide. Please try again.", "Impossible de charger le guide. Réessayez."))
        }
    }

    suspend fun delete(id: String) {
        try {
            ApiClient.client.delete("/api/guides/$id")
        } catch (e: Exception) {
            println("Guide delete error: ${e.message}")
            throw Exception(Msg.t("Could not delete the guide.", "Impossible de supprimer le guide."))
        }
    }

    suspend fun downloadPdf(id: String): ByteArray {
        try {
            return ApiClient.client.get("/api/guides/$id/pdf").body()
        } catch (e: Exception) {
            println("Guide PDF error: ${e.message}")
            throw Exception(Msg.t("Could not download the PDF.", "Impossible de télécharger le PDF."))
        }
    }
}
