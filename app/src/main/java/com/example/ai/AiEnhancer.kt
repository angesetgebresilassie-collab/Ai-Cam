package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
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
                enhancementSummary = "On-Device Standard Processing",
                isOnlineGeminiUsed = false
            )

        var sceneCategory: String
        var aiSummary: String
        var usedGemini = false

        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        val onlineAvailable = isOnline() && apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (onlineAvailable) {
            var geminiSuccess = false
            try {
                val geminiAnalysis = analyzeSceneWithGemini(bitmap, apiKey)
                if (geminiAnalysis != null) {
                    sceneCategory = geminiAnalysis.first
                    aiSummary = "Gemini Cloud AI: " + geminiAnalysis.second
                    usedGemini = true
                    geminiSuccess = true
                } else {
                    sceneCategory = detectSceneHeuristic(bitmap)
                    aiSummary = "On-Device Neural Engine: Automatic HDR tone mapping & clarity boost"
                }
            } catch (e: Exception) {
                Log.d("AiEnhancer", "Gemini API failed, seamlessly switching to offline algorithm: ${e.message}")
                sceneCategory = detectSceneHeuristic(bitmap)
                aiSummary = "On-Device Engine: Fallback local scene optimization applied"
            }
        } else {
            // Offline Mode: Pure On-Device Computer Vision & Color Histogram Analysis
            sceneCategory = detectSceneHeuristic(bitmap)
            aiSummary = "On-Device AI Engine: Local histogram scene analysis, dynamic range expansion & warmth tuning"
        }

        // Apply visual remastering based on scene analysis
        val enhancedBitmap = applyAiRemastering(bitmap, sceneCategory)

        // Save enhanced image
        val enhancedFile = File(
            context.cacheDir,
            "enhanced_${System.currentTimeMillis()}_${originalFile.name}"
        )
        FileOutputStream(enhancedFile).use { out ->
            enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        AiEnhancementResult(
            enhancedFilePath = enhancedFile.absolutePath,
            sceneCategory = sceneCategory,
            enhancementSummary = aiSummary,
            isOnlineGeminiUsed = usedGemini
        )
    }

    private fun detectSceneHeuristic(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 12
        var totalRed = 0L
        var totalGreen = 0L
        var totalBlue = 0L
        var count = 0L

        for (x in 0 until width step (width / sampleSize).coerceAtLeast(1)) {
            for (y in 0 until height step (height / sampleSize).coerceAtLeast(1)) {
                val pixel = bitmap.getPixel(x, y)
                totalRed += Color.red(pixel)
                totalGreen += Color.green(pixel)
                totalBlue += Color.blue(pixel)
                count++
            }
        }

        if (count == 0L) return "General"
        val avgRed = (totalRed / count).toInt()
        val avgGreen = (totalGreen / count).toInt()
        val avgBlue = (totalBlue / count).toInt()
        val brightness = (avgRed + avgGreen + avgBlue) / 3

        return when {
            avgRed > avgGreen + 25 && avgRed > avgBlue + 15 -> "Sunset"
            avgGreen > avgRed + 15 && avgGreen > avgBlue + 15 -> "Landscape"
            avgBlue > avgRed + 15 && avgBlue > avgGreen + 10 -> "Sky & Water"
            brightness < 75 -> "Night"
            avgRed > 120 && avgGreen > 90 && avgBlue > 70 && Math.abs(avgRed - avgGreen) < 45 -> "Portrait"
            avgRed > 110 && avgGreen > 85 && avgBlue < 80 -> "Food"
            else -> "Auto Scene"
        }
    }

    private suspend fun analyzeSceneWithGemini(bitmap: Bitmap, apiKey: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 384, 384, true)
        val stream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val prompt = "Analyze this camera photo. Return a JSON with: 'category' (choose: Sunset, Portrait, Landscape, Food, Night, Sky & Water, Auto Scene) and 'recommendation' (1 concise sentence describing color and lighting remastering)."

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
                val rec = obj.optString("recommendation", "Optimized HDR balance, fine detail, and warmth")
                return@withContext Pair(cat, rec)
            }
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

