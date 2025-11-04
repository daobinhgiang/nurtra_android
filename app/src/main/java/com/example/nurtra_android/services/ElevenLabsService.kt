package com.example.nurtra_android.services

import android.util.Log
import com.example.nurtra_android.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Service to interact with ElevenLabs API for text-to-speech generation
 */
class ElevenLabsService {
    private val client = OkHttpClient()
    private val apiKey = BuildConfig.ELEVENLABS_API_KEY
    private val TAG = "ElevenLabsService"

    companion object {
        private const val ELEVENLABS_BASE_URL = "https://api.elevenlabs.io/v1"
        // Default voice ID - Rachel (a pleasant, neutral voice)
        private const val DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM"
    }

    /**
     * Generates speech audio from text using ElevenLabs TTS API
     * @param text The text to convert to speech
     * @param voiceId Optional voice ID (defaults to Rachel)
     * @return Result containing audio data as ByteArray (MP3 format)
     */
    suspend fun generateSpeech(
        text: String,
        voiceId: String? = null
    ): Result<ByteArray> {
        return try {
            val actualVoiceId = voiceId ?: DEFAULT_VOICE_ID
            Log.d(TAG, "Generating speech for text (length: ${text.length} chars) with voice: $actualVoiceId")
            Log.d(TAG, "Text preview: ${text.take(50)}${if (text.length > 50) "..." else ""}")
            val audioData = callElevenLabsAPI(text, actualVoiceId)
            Log.d(TAG, "Speech generation completed successfully, audio size: ${audioData.size} bytes")
            Result.success(audioData)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating speech: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Makes API call to ElevenLabs text-to-speech endpoint
     */
    private suspend fun callElevenLabsAPI(
        text: String,
        voiceId: String
    ): ByteArray = suspendCoroutine { continuation ->
        Log.d(TAG, "Preparing ElevenLabs API request...")
        Log.d(TAG, "API Key present: ${apiKey.isNotEmpty()}")
        Log.d(TAG, "Voice ID: $voiceId")
        
        val jsonBody = JSONObject().apply {
            put("text", text)
            put("model_id", "eleven_monolingual_v1")
            put("voice_settings", JSONObject().apply {
                put("stability", 0.5)
                put("similarity_boost", 0.75)
            })
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val url = "$ELEVENLABS_BASE_URL/text-to-speech/$voiceId"
        Log.d(TAG, "Making request to: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("xi-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "audio/mpeg")
            .post(requestBody)
            .build()

        Log.d(TAG, "Sending request to ElevenLabs API...")
        val startTime = System.currentTimeMillis()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "ElevenLabs API call failed after ${duration}ms: ${e.message}", e)
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val duration = System.currentTimeMillis() - startTime
                try {
                    Log.d(TAG, "Received response from ElevenLabs API (${duration}ms), status: ${response.code}")
                    
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        Log.e(TAG, "ElevenLabs API error: ${response.code} - $errorBody")
                        continuation.resumeWithException(
                            Exception("ElevenLabs API error: ${response.code}")
                        )
                        return
                    }

                    val audioBytes = response.body?.bytes()
                    if (audioBytes == null) {
                        Log.e(TAG, "Empty response body from ElevenLabs")
                        continuation.resumeWithException(
                            Exception("Empty response from ElevenLabs")
                        )
                        return
                    }

                    Log.d(TAG, "Successfully generated speech audio (${audioBytes.size} bytes) in ${duration}ms")
                    continuation.resume(audioBytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing ElevenLabs response after ${duration}ms", e)
                    continuation.resumeWithException(e)
                }
            }
        })
    }
}

