package com.example.nurtra_android.services

import android.util.Log
import com.example.nurtra_android.BuildConfig
import com.example.nurtra_android.data.OnboardingSurveyResponses
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Service to interact with OpenAI API for generating personalized motivational quotes
 */
class OpenAIService {
    private val client = OkHttpClient()
    private val apiKey = BuildConfig.OPENAI_API_KEY
    private val TAG = "OpenAIService"

    companion object {
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val MODEL = "gpt-4o-mini"
    }

    /**
     * Generates 10 personalized motivational quotes based on user's onboarding survey responses
     */
    suspend fun generatePersonalizedQuotes(responses: OnboardingSurveyResponses): Result<List<String>> {
        return try {
            val prompt = buildPrompt(responses)
            val quotes = callOpenAI(prompt)
            Result.success(quotes)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating quotes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Builds a personalized prompt based on user's survey responses
     */
    private fun buildPrompt(responses: OnboardingSurveyResponses): String {
        val promptBuilder = StringBuilder()
        promptBuilder.append("You are a compassionate therapist helping someone overcome binge eating. ")
        promptBuilder.append("Generate 10 personalized, supportive motivational quotes based on their responses:\n\n")

        if (responses.struggleDuration.isNotEmpty()) {
            promptBuilder.append("Duration of struggle: ${responses.struggleDuration.joinToString(", ")}\n")
        }
        if (responses.bingeFrequency.isNotEmpty()) {
            promptBuilder.append("Frequency: ${responses.bingeFrequency.joinToString(", ")}\n")
        }
        if (responses.importanceReason.isNotEmpty()) {
            promptBuilder.append("Why recovery is important: ${responses.importanceReason.joinToString(", ")}\n")
        }
        if (responses.lifeWithoutBinge.isNotEmpty()) {
            promptBuilder.append("Vision for life: ${responses.lifeWithoutBinge.joinToString(", ")}\n")
        }
        if (responses.bingeThoughts.isNotEmpty()) {
            promptBuilder.append("Common thoughts: ${responses.bingeThoughts.joinToString(", ")}\n")
        }
        if (responses.bingeTriggers.isNotEmpty()) {
            promptBuilder.append("Triggers: ${responses.bingeTriggers.joinToString(", ")}\n")
        }
        if (responses.whatMattersMost.isNotEmpty()) {
            promptBuilder.append("What matters most: ${responses.whatMattersMost.joinToString(", ")}\n")
        }
        if (responses.recoveryValues.isNotEmpty()) {
            promptBuilder.append("Recovery values: ${responses.recoveryValues.joinToString(", ")}\n")
        }

        promptBuilder.append("\nGenerate exactly 10 motivational quotes (one per line) that:\n")
        promptBuilder.append("- Are personalized to their specific situation and values\n")
        promptBuilder.append("- Address their triggers and thoughts with compassion\n")
        promptBuilder.append("- Reinforce their vision and why recovery matters to them\n")
        promptBuilder.append("- Are concise (1-2 sentences max)\n")
        promptBuilder.append("- Use encouraging, non-judgmental language\n")
        promptBuilder.append("- Focus on strength, hope, and progress\n")
        promptBuilder.append("\nProvide ONLY the 10 quotes, one per line, with no numbering or extra formatting.")

        return promptBuilder.toString()
    }

    /**
     * Makes API call to OpenAI and parses the response
     */
    private suspend fun callOpenAI(prompt: String): List<String> = suspendCoroutine { continuation ->
        val jsonBody = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a compassionate therapist helping people overcome binge eating.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
            put("max_tokens", 1000)
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(OPENAI_API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "OpenAI API call failed", e)
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    
                    if (!response.isSuccessful) {
                        Log.e(TAG, "OpenAI API error: ${response.code} - $responseBody")
                        continuation.resumeWithException(
                            Exception("OpenAI API error: ${response.code}")
                        )
                        return
                    }

                    if (responseBody == null) {
                        continuation.resumeWithException(Exception("Empty response from OpenAI"))
                        return
                    }

                    val jsonResponse = JSONObject(responseBody)
                    val content = jsonResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    // Parse quotes from response (one per line)
                    val quotes = content.lines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .take(10) // Ensure we only get 10 quotes

                    if (quotes.size < 10) {
                        Log.w(TAG, "Received ${quotes.size} quotes instead of 10")
                    }

                    Log.d(TAG, "Successfully generated ${quotes.size} quotes")
                    continuation.resume(quotes)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing OpenAI response", e)
                    continuation.resumeWithException(e)
                }
            }
        })
    }
}

