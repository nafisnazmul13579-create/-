package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ImageGenerator {
    private const val TAG = "ImageGenerator"

    // 10 free models mapped to their Pollinations API identifiers
    val modelMap = mapOf(
        "flux" to "flux",
        "flux-realism" to "flux-realism",
        "flux-anime" to "flux-anime",
        "flux-3d" to "flux-3d",
        "turbo" to "turbo",
        "dreamshaper" to "dreamshaper",
        "absolute-reality" to "absolute-reality",
        "deliberate" to "deliberate",
        "midjourney-style" to "midjourney-style",
        "cyberpunk" to "cyberpunk"
    )

    fun getImageUrl(prompt: String, model: String, is4K: Boolean, seed: Int): String {
        val mappedModel = modelMap[model] ?: "flux"
        val width = if (is4K) 2048 else 1024 // Pollinations max width is 2048 (approx 2K/4K scaling), standard HD is 1024
        val height = if (is4K) 1152 else 576  // 16:9 ratio
        val encodedPrompt = URLEncoder.encode(prompt, "UTF-8")
        return "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&model=$mappedModel&seed=$seed&nologo=true"
    }

    suspend fun generateImageWithFallback(prompt: String, model: String, seed: Int): Pair<String, String> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()

        val url4K = getImageUrl(prompt, model, is4K = true, seed = seed)
        val urlHD = getImageUrl(prompt, model, is4K = false, seed = seed)

        try {
            // Attempt 4K first
            val request = Request.Builder().url(url4K).head().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully generated 4K image reference!")
                    return@withContext Pair(url4K, "4K")
                } else {
                    Log.e(TAG, "4K request failed with code ${response.code}, falling back to HD.")
                    return@withContext Pair(urlHD, "HD")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "4K generation timed out or failed, automatically falling back to HD", e)
            return@withContext Pair(urlHD, "HD")
        }
    }
}
