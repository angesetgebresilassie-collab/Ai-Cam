package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AiEnhancementResult(
    val enhancedFilePath: String,
    val sceneCategory: String,
    val enhancementSummary: String,
    val isOnlineGeminiUsed: Boolean
)

class AiEnhancer(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun enhancePhotoSilently(originalFile: File): AiEnhancementResult = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
            ?: return@withContext AiEnhancementResult(
                enhancedFilePath = originalFile.absolutePath,
                sceneCategory = "Standard",
                enhancementSummary = "Standard Photo Capture",
                isOnlineGeminiUsed = false
            )

        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        val canCallGemini = isOnline() && apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (canCallGemini) {
            try {
                val geminiAnalysis = analyzeSceneWithGemini(bitmap, apiKey)
                if (geminiAnalysis != null) {
                    val sceneCategory = geminiAnalysis.first
                    val aiSummary = "Gemini Cloud AI: " + geminiAnalysis.second

                    val enhancedBitmap = applyAiRemastering(bitmap, sceneCategory)

                    val enhancedFile = File(
                        context.cacheDir,
                        "enhanced_${System.currentTimeMillis()}_${originalFile.name}"
                    )
                    FileOutputStream(enhancedFile).use { out ->
                        enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }

                    return@withContext AiEnhancementResult(
                        enhancedFilePath = enhancedFile.absolutePath,
                        sceneCategory = sceneCategory,
                        enhancementSummary = aiSummary,
                        isOnlineGeminiUsed = true
                    )
                }
            } catch (e: Exception) {
                Log.d("AiEnhancer", "Gemini API call failed: ${e.message}")
            }
        }

        // Strictly no offline mechanism - return original photo as-is if Gemini API is offline/unavailable
        return@withContext AiEnhancementResult(
            enhancedFilePath = originalFile.absolutePath,
            sceneCategory = "Standard Photo",
            enhancementSummary = "Original Capture (Gemini AI active when online)",
            isOnlineGeminiUsed = false
        )
    }

    private suspend fun analyzeSceneWithGemini(bitmap: Bitmap, apiKey: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 384, 384, true)
        val stream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val prompt = "Analyze this camera photo. Output a JSON object with keys 'category' (choose from: Sunset, Portrait, Landscape, Food, Night, Sky & Water, Auto Scene) and 'recommendation' (1 short sentence describing the remastered lighting, color tone, and HDR adjustments)."

        val jsonRequest = JSONObject().apply {
            put("contents", org.json.JSONArray().put(JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(JSONObject().put("text", prompt))
                    put(JSONObject().put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    }))
                })
            }))
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body?.string() ?: return@withContext null
            val jsonResp = JSONObject(body)
            val text = jsonResp.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}")
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val cleanJsonStr = text.substring(jsonStart, jsonEnd + 1)
                val obj = JSONObject(cleanJsonStr)
                val cat = obj.optString("category", "Auto Scene")
                val rec = obj.optString("recommendation", "Enhanced dynamic range, warmth, and color accuracy")
                return@withContext Pair(cat, rec)
            }
        } else {
            Log.e("AiEnhancer", "Gemini HTTP error code: ${response.code}")
        }
        return@withContext null
    }

    private fun applyAiRemastering(original: Bitmap, scene: String): Bitmap {
        val width = original.width
        val height = original.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cm = ColorMatrix()

        when (scene) {
            "Sunset" -> {
                cm.set(floatArrayOf(
                    1.28f, 0.05f, 0.00f, 0f, 14f,
                    0.05f, 1.18f, 0.00f, 0f, 8f,
                    0.00f, 0.00f, 1.05f, 0f, -4f,
                    0.00f, 0.00f, 0.00f, 1f, 0f
                ))
            }
            "Portrait" -> {
                cm.set(floatArrayOf(
                    1.14f, 0.02f, 0.00f, 0f, 10f,
                    0.02f, 1.10f, 0.00f, 0f, 6f,
                    0.00f, 0.02f, 1.06f, 0f, 4f,
                    0.00f, 0.00f, 0.00f, 1f, 0f
                ))
            }
            "Landscape", "Sky & Water" -> {
                cm.set(floatArrayOf(
                    1.10f, 0.00f, 0.00f, 0f, 2f,
                    0.00f, 1.25f, 0.00f, 0f, 6f,
                    0.00f, 0.00f, 1.28f, 0f, 10f,
                    0.00f, 0.00f, 0.00f, 1f, 0f
                ))
            }
            "Night" -> {
                cm.set(floatArrayOf(
                    1.22f, 0.00f, 0.00f, 0f, 22f,
                    0.00f, 1.22f, 0.00f, 0f, 22f,
                    0.00f, 0.00f, 1.25f, 0f, 25f,
                    0.00f, 0.00f, 0.00f, 1f, 0f
                ))
            }
            "Food" -> {
                cm.set(floatArrayOf(
                    1.22f, 0.00f, 0.00f, 0f, 12f,
                    0.00f, 1.20f, 0.00f, 0f, 8f,
                    0.00f, 0.00f, 1.08f, 0f, 2f,
                    0.00f, 0.00f, 0.00f, 1f, 0f
                ))
            }
            else -> {
                cm.set(floatArrayOf(
                    1.16f, 0.00f, 0.00f, 0f, 6f,
                    0.00f, 1.16f, 0.00f, 0f, 6f,
                    0.00f, 0.00f, 1.16f, 0f, 6f,
                    0.00f, 0.00f, 0.00f, 1f, 0f
                ))
            }
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return output
    }
}

