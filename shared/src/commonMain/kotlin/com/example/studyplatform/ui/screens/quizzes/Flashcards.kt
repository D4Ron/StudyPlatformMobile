package com.example.studyplatform.ui.screens.quizzes

import com.example.studyplatform.api.AppJson
import com.example.studyplatform.model.QuizResponse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Question on the front, the correct answer on the back. */
internal data class Flashcard(val front: String, val back: String, val note: String?)

/**
 * Turns a quiz payload into cards.
 *
 * Separate from the screen so it can be tested. The payload is author-authored JSON
 * whose shape the client does not control, and every branch below exists because some
 * shape of it would otherwise produce a card with a blank back — which teaches nothing
 * and reads as a bug.
 *
 * Reads through [AppJson] rather than `ApiClient.json`: the two are the same
 * configuration, but the latter drags the HTTP client and the settings store in behind
 * it, which is exactly what `AppJson` was split out to avoid.
 */
internal fun quizToCards(quiz: QuizResponse): List<Flashcard> = try {
    val root = AppJson.instance.parseToJsonElement(quiz.questions.toString())
    val array = when {
        root is JsonObject && root.containsKey("questions") -> root["questions"]!!.jsonArray
        root is JsonArray -> root
        else -> JsonArray(emptyList())
    }

    array.mapNotNull { element ->
        val q = element as? JsonObject ?: return@mapNotNull null

        val front = q["questionText"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: return@mapNotNull null

        val options = q["options"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: emptyList()

        val answer = q["correctAnswer"]?.jsonPrimitive?.contentOrNull?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return@mapNotNull null

        val back = backFor(answer, options) ?: return@mapNotNull null

        Flashcard(
            front = front,
            back = back,
            note = q["explanation"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        )
    }
} catch (_: Exception) {
    // A payload that will not parse yields no cards rather than taking the screen down.
    emptyList()
}

/**
 * The answer text, from whatever the author put in `correctAnswer`.
 *
 * Normally a letter indexing [options] — that is what the generator emits and what the
 * take screen and the server score against. Two other shapes are accepted because they
 * occur and the alternative is silently dropping every card:
 *
 * - **The answer text itself**, matched case-insensitively against the options. A
 *   generator that emits text rather than a letter would otherwise resolve to some
 *   unrelated index, or none, and lose the whole deck.
 * - **No options at all**, for question types that carry the answer directly.
 *
 * A letter that points past the end of the options resolves to nothing, and the caller
 * drops the card. That is the honest outcome: the payload is wrong and there is no
 * answer to show.
 */
private fun backFor(answer: String, options: List<String>): String? {
    if (options.isEmpty()) return answer

    options.firstOrNull { it.equals(answer, ignoreCase = true) }?.let { return it }

    if (answer.length == 1) {
        val index = answer[0].uppercaseChar() - 'A'
        return options.getOrNull(index)
    }
    return null
}
