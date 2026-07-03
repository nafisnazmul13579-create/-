package com.example.network

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

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun splitStoryIntoScenes(story: String): List<ParsedScene> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is empty or placeholder!")
            return@withContext fallbackSplit(story)
        }

        val prompt = """
            You are HM AI, a professional storyboarding assistant. Split the following story into individual scenes based on sentences. For each scene, generate:
            1. The exact sentence text.
            2. A highly descriptive, optimized image generation prompt (max 60 words). Include recommendations for style, lighting, camera angle, and detail, but do not include metadata or camera setting keywords like '4K' or 'HD'.
            3. An optimized video prompt describing the motion, camera movement (e.g., panning, zooming, light leak), and focus.
            4. Recommended narration text (voiceover) based on the sentence.
            5. Estimated narration duration in seconds (based on around 2.5 to 3 words per second, min 3 seconds, max 12 seconds).
            6. Recommended best-matching free AI image model from these 10 choices:
               - 'flux' (best for general fast illustrations/concepts)
               - 'flux-realism' (best for lifelike photographs/people)
               - 'flux-anime' (best for Japanese anime/manga)
               - 'flux-3d' (best for 3D Pixar/CGI renders)
               - 'turbo' (best for abstract, quick concept art)
               - 'dreamshaper' (best for epic fantasy, sci-fi art)
               - 'absolute-reality' (best for dramatic cinematic realism)
               - 'deliberate' (best for artistic painting/portraits)
               - 'midjourney-style' (best for dramatic lighting and cinematic composition)
               - 'cyberpunk' (best for neon sci-fi and dystopian themes)

            Your response must be a valid JSON array of objects, each representing a scene with these exact keys:
            "sentence" (string)
            "imagePrompt" (string)
            "videoPrompt" (string)
            "narrationText" (string)
            "durationSeconds" (integer)
            "modelName" (string - must be exactly one of the 10 choices listed above)

            Do not include any markdown backticks or other text outside the JSON array. Output ONLY the JSON array.
            
            Story: $story
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed: ${response.code} ${response.message}")
                    return@withContext fallbackSplit(story)
                }

                val bodyStr = response.body?.string() ?: return@withContext fallbackSplit(story)
                val responseJson = JSONObject(bodyStr)
                val candidates = responseJson.getJSONArray("candidates")
                val textResponse = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val parsedScenes = mutableListOf<ParsedScene>()
                val array = JSONArray(textResponse.trim())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parsedScenes.add(
                        ParsedScene(
                            sentence = obj.optString("sentence", ""),
                            imagePrompt = obj.optString("imagePrompt", ""),
                            videoPrompt = obj.optString("videoPrompt", ""),
                            narrationText = obj.optString("narrationText", ""),
                            durationSeconds = obj.optInt("durationSeconds", 5),
                            modelName = obj.optString("modelName", "flux")
                        )
                    )
                }
                if (parsedScenes.isEmpty()) {
                    return@withContext fallbackSplit(story)
                }
                parsedScenes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error", e)
            fallbackSplit(story)
        }
    }

    private fun fallbackSplit(story: String): List<ParsedScene> {
        val sentences = story.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val models = listOf("flux", "flux-realism", "flux-anime", "flux-3d", "turbo", "dreamshaper", "absolute-reality", "deliberate", "midjourney-style", "cyberpunk")

        return sentences.mapIndexed { index, sentence ->
            val cleanSentence = sentence.replace(Regex("[^a-zA-Z0-9\\s]"), "")
            val wordCount = sentence.split("\\s+".toRegex()).size
            val duration = (wordCount / 2).coerceIn(3, 10)
            ParsedScene(
                sentence = sentence,
                imagePrompt = "$sentence, highly detailed, cinematic lighting, sharp focus",
                videoPrompt = "Cinematic slow motion pan zoom effect",
                narrationText = sentence,
                durationSeconds = duration,
                modelName = models[index % models.size]
            )
        }
    }
}

data class ParsedScene(
    val sentence: String,
    val imagePrompt: String,
    val videoPrompt: String,
    val narrationText: String,
    val durationSeconds: Int,
    val modelName: String
)
