package dev.privatebutts.hrrelay

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class HeartRateClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor { Log.d(TAG, it) }
            .apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun send(serverUrl: String, bpm: Double) {
        if (serverUrl.isBlank()) return

        val body = JSONObject().apply {
            put("bpm", bpm.toInt())
            put("ts", System.currentTimeMillis())
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(serverUrl)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Sent bpm=$bpm -> ${response.code}")
                if (!response.isSuccessful) {
                    throw RuntimeException("Server returned ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Send failed to $serverUrl: ${e.localizedMessage}")
            throw e
        }
    }

    companion object {
        private const val TAG = "HeartRateClient"
    }
}
