package com.thrustwealth.app.data.repository

import com.thrustwealth.app.data.model.JumpSearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JumpRepository @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun searchJump(keyword: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedKeyword = URLEncoder.encode(keyword.trim(), "UTF-8")
            val requestUrl = "https://www.thrustwealth.com/index/jump/search?keyword=$encodedKeyword"
            val request = Request.Builder()
                .url(requestUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP error code: ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val jumpResponse = json.decodeFromString<JumpSearchResponse>(body)
                if (jumpResponse.code == 1 && !jumpResponse.data?.url.isNullOrBlank()) {
                    val targetUrl = jumpResponse.data.url.orEmpty()
                    preferencesRepository.setSavedWebUrl(targetUrl)
                    targetUrl
                } else {
                    null
                }
            }
        }
    }
}
