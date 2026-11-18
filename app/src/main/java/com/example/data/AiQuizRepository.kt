package com.example.data

import android.util.Log
import com.example.model.QuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GeneratedQuizSet(
    val title: String,
    val topic: String,
    val questions: List<QuizQuestion>
)

class AiQuizRepository {
    private val quizSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Catchy title for this trivia challenge"),
            "topic" to Schema.string(description = "Primary category/topic of the questions"),
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(description = "Clear, engaging trivia question"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 distinct answer choices"
                        ),
                        "correctIndex" to Schema.integer(description = "Zero-based index (0 to 3) of the correct option")
                    )
                ),
                description = "List of trivia questions"
            )
        )
    )

    private val singleQuestionSchema = Schema.obj(
        properties = mapOf(
            "question" to Schema.string(description = "Clear trivia question"),
            "options" to Schema.array(
                items = Schema.string(),
                description = "Exactly 4 distinct answer choices"
            ),
            "correctIndex" to Schema.integer(description = "Zero-based index (0 to 3) of the correct option")
        )
    )

    private val quizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    private val questionAssistModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = singleQuestionSchema
            }
        )
    }

    suspend fun generateRandomQuiz(
        topic: String,
        difficulty: String,
        questionCount: Int = 5
    ): Result<GeneratedQuizSet> {
        return try {
            val seed = System.currentTimeMillis()
            val topicInstruction = if (topic == "Surprise Me") {
                "a surprising variety of fun topics (science, pop culture, geography, gaming, history, tech)"
            } else {
                topic
            }
            val prompt = """
                Generate a fresh, accurate $difficulty trivia quiz with exactly $questionCount multiple-choice questions on $topicInstruction.
                Each question MUST have exactly 4 short, clear answer choices and a valid correctIndex between 0 and 3.
                Keep questions fun, unambiguous, and varied. Random seed: $seed.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                quizModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val root = JSONObject(rawJson)
            val title = root.optString("title", "$topic Trivia Blitz").ifBlank { "$topic Trivia Blitz" }
            val resolvedTopic = root.optString("topic", topic).ifBlank { topic }
            val qArray = root.getJSONArray("questions")
            val parsedQuestions = mutableListOf<QuizQuestion>()
            for (i in 0 until qArray.length()) {
                val item = qArray.getJSONObject(i)
                val qText = item.getString("question").trim()
                val optArray = item.getJSONArray("options")
                val opts = (0 until optArray.length()).map { optArray.getString(it).trim() }.take(4)
                val correctIdx = item.optInt("correctIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0))
                if (qText.isNotBlank() && opts.size == 4) {
                    parsedQuestions.add(QuizQuestion(question = qText, options = opts, correctIndex = correctIdx))
                }
            }
            if (parsedQuestions.isEmpty()) {
                throw IllegalStateException("No valid trivia questions were generated.")
            }
            Result.success(GeneratedQuizSet(title = title, topic = resolvedTopic, questions = parsedQuestions))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI quiz generation timed out", e)
            Result.failure(Exception("Quiz generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI quiz generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateSingleQuestion(topic: String): Result<QuizQuestion> {
        return try {
            val seed = System.currentTimeMillis()
            val prompt = "Create 1 clever tournament trivia question about $topic with 4 distinct options and correctIndex (0..3). Seed: $seed."
            val response = withTimeout(45_000L) {
                questionAssistModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val obj = JSONObject(rawJson)
            val qText = obj.getString("question").trim()
            val optArray = obj.getJSONArray("options")
            val opts = (0 until optArray.length()).map { optArray.getString(it).trim() }.take(4)
            val correctIdx = obj.optInt("correctIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0))
            if (qText.isBlank() || opts.size != 4) {
                throw IllegalStateException("Generated question format was incomplete.")
            }
            Result.success(QuizQuestion(question = qText, options = opts, correctIndex = correctIdx))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI question assist timed out", e)
            Result.failure(Exception("Question generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI question assist failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiQuizRepository"
    }
}
