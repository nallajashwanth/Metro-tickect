package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val SYSTEM_INSTRUCTION = """
        You are "Hyderabad Metro AI", an intelligent metro assistant. You converse in English, Telugu, and Hindi depending on the user's language or preference.
        You help passengers with:
        1. Fares: Base ₹10, increases by ~₹3 per station up to Max ₹60.
        2. Routes: Help passengers find station routes, indicating line transfers at Ameerpet (Red/Blue), MG Bus Station (Red/Green), or Parade Ground (Blue/Green).
        3. Landmarks: Provide guidance for popular tourist places (e.g. Charminar -> near MGBS/Nampally, Golconda -> near Jubilee Hills, HITEC City -> Cyber Towers/Mindspace).
        4. Booking: Direct booking from source to destination.
        
        CRITICAL RULES:
        - If the user explicitly asks to book a ticket (e.g., "book ticket from Ameerpet to Miyapur" or "Ameerpet to Miyapur ticket book cheyyi"), you MUST end your message with exactly: `[BOOK: Source to Destination]` where Source and Destination are valid Hyderabad Metro station names. 
        Example response: "I can help you book that. I've initiated the ticket booking for you. [BOOK: Ameerpet to Miyapur]"
        
        Keep responses helpful, friendly, and concise. Format lists cleanly.
    """.trimIndent()

    suspend fun getAiResponse(userPrompt: String, history: List<Pair<String, String>> = emptyList()): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is not configured. Please add GEMINI_API_KEY in the Secrets panel in AI Studio."
        }

        try {
            val requestJson = JSONObject()
            
            // Build contents array (history + current prompt)
            val contentsArray = JSONArray()
            
            for (turn in history) {
                val userTurn = JSONObject()
                userTurn.put("role", "user")
                val userParts = JSONArray()
                userParts.put(JSONObject().put("text", turn.first))
                userTurn.put("parts", userParts)
                contentsArray.put(userTurn)
                
                val modelTurn = JSONObject()
                modelTurn.put("role", "model")
                val modelParts = JSONArray()
                modelParts.put(JSONObject().put("text", turn.second))
                modelTurn.put("parts", modelParts)
                contentsArray.put(modelTurn)
            }
            
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", userPrompt))
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)
            
            requestJson.put("contents", contentsArray)

            // Add system instruction
            val systemInstructionJson = JSONObject()
            val systemParts = JSONArray()
            systemParts.put(JSONObject().put("text", SYSTEM_INSTRUCTION))
            systemInstructionJson.put("parts", systemParts)
            requestJson.put("systemInstruction", systemInstructionJson)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API call failed: $errBody")
                    return@withContext "Error: Request failed with status ${response.code}"
                }

                val responseBody = response.body?.string() ?: return@withContext "Empty response from Gemini AI."
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No text part found.")
                        }
                    }
                }
                return@withContext "Could not interpret Gemini response."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAiResponse", e)
            return@withContext "Error interacting with AI Assistant: ${e.localizedMessage}"
        }
    }
}
