package com.example.studyplatform.ui.screens.quizzes

import com.example.studyplatform.api.AppJson
import com.example.studyplatform.model.QuizResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Building flashcards from a quiz payload.
 *
 * The payload is author-authored JSON the client does not control, and the failure mode
 * is quiet: a card whose answer cannot be resolved is dropped, so a wrong assumption
 * here does not crash — it silently produces a shorter deck, or an empty one, and looks
 * like the quiz simply had nothing in it.
 */
class FlashcardsTest {

    private fun quiz(questionsJson: String) = QuizResponse(
        id = "q1",
        title = "Test",
        topic = "Test",
        difficulty = "EASY",
        quizType = "STANDARD",
        questionCount = 1,
        questions = AppJson.instance.parseToJsonElement(questionsJson)
    )

    @Test
    fun `resolves the answer letter to its option`() {
        val cards = quizToCards(quiz("""
            [{"questionText":"Capital of Togo?",
              "options":["Accra","Lome","Cotonou","Abidjan"],
              "correctAnswer":"B",
              "explanation":"Lome is on the coast."}]
        """))

        assertEquals(1, cards.size)
        assertEquals("Capital of Togo?", cards[0].front)
        assertEquals("Lome", cards[0].back)
        assertEquals("Lome is on the coast.", cards[0].note)
    }

    @Test
    fun `accepts a questions wrapper object as well as a bare array`() {
        val wrapped = quizToCards(quiz("""
            {"questions":[{"questionText":"A?","options":["x","y"],"correctAnswer":"A"}]}
        """))
        assertEquals(1, wrapped.size)
        assertEquals("x", wrapped[0].back)
    }

    @Test
    fun `a lowercase answer letter still resolves`() {
        val cards = quizToCards(quiz("""
            [{"questionText":"A?","options":["x","y","z"],"correctAnswer":"c"}]
        """))
        assertEquals("z", cards[0].back)
    }

    @Test
    fun `the answer text is accepted where a letter was expected`() {
        // A generator emitting text rather than a letter would otherwise index by its
        // first character and lose the entire deck.
        val cards = quizToCards(quiz("""
            [{"questionText":"Capital?","options":["Accra","Lome"],"correctAnswer":"Lome"}]
        """))

        assertEquals(1, cards.size)
        assertEquals("Lome", cards[0].back)
    }

    @Test
    fun `matching the answer text ignores case`() {
        val cards = quizToCards(quiz("""
            [{"questionText":"Capital?","options":["Accra","Lome"],"correctAnswer":"lome"}]
        """))
        assertEquals("Lome", cards[0].back)
    }

    @Test
    fun `a letter past the end of the options drops the card`() {
        val cards = quizToCards(quiz("""
            [{"questionText":"A?","options":["x","y"],"correctAnswer":"E"}]
        """))
        assertTrue(cards.isEmpty())
    }

    @Test
    fun `a question with no options carries the answer directly`() {
        val cards = quizToCards(quiz("""
            [{"questionText":"Name the capital.","correctAnswer":"Lome"}]
        """))
        assertEquals("Lome", cards[0].back)
    }

    @Test
    fun `a question with no text is dropped`() {
        val cards = quizToCards(quiz("""
            [{"options":["x"],"correctAnswer":"A"},
             {"questionText":"   ","options":["x"],"correctAnswer":"A"}]
        """))
        assertTrue(cards.isEmpty())
    }

    @Test
    fun `a question with no answer is dropped`() {
        val cards = quizToCards(quiz("""
            [{"questionText":"A?","options":["x","y"]}]
        """))
        assertTrue(cards.isEmpty())
    }

    @Test
    fun `good questions survive alongside broken ones`() {
        // One bad entry must not cost the student the rest of the deck.
        val cards = quizToCards(quiz("""
            [{"questionText":"Good?","options":["x","y"],"correctAnswer":"A"},
             {"questionText":"Bad?","options":["x"],"correctAnswer":"Z"},
             {"questionText":"Also good?","options":["p","q"],"correctAnswer":"B"}]
        """))

        assertEquals(2, cards.size)
        assertEquals(listOf("Good?", "Also good?"), cards.map { it.front })
    }

    @Test
    fun `a blank explanation becomes no note rather than an empty line`() {
        val cards = quizToCards(quiz("""
            [{"questionText":"A?","options":["x"],"correctAnswer":"A","explanation":"  "}]
        """))
        assertNull(cards[0].note)
    }

    @Test
    fun `unparseable questions yield no cards rather than throwing`() {
        val cards = quizToCards(
            QuizResponse(
                id = "q1", title = "t", topic = "t",
                difficulty = "EASY", quizType = "STANDARD",
                questionCount = 0, questions = null
            )
        )
        assertTrue(cards.isEmpty())
    }

    @Test
    fun `an answer with surrounding whitespace still resolves`() {
        val cards = quizToCards(quiz("""
            [{"questionText":"A?","options":["x","y"],"correctAnswer":"  B  "}]
        """))
        assertEquals("y", cards[0].back)
    }
}
