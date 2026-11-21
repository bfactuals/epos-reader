package com.example.engine.config

import com.example.config.APP_META
import com.example.config.CreatorLink
import com.example.config.REMOTE_CONFIG_URL
import com.example.config.RemoteAppConfig
import com.example.config.isSafeExternalUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Silent zero-cost remote config loader backed by GitHub raw JSON + local SharedPreferences cache.
 * Never displays errors when offline or unreachable.
 */
object RemoteConfigService {

    suspend fun fetchLatestRawJson(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val cacheBustedUrl = "$REMOTE_CONFIG_URL?t=${System.currentTimeMillis()}"
            val conn = (URL(cacheBustedUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                // Verify JSON parses cleanly before caching
                if (parseJson(body) != null) body else null
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    fun parseJson(rawJson: String?): RemoteAppConfig? {
        if (rawJson.isNullOrBlank()) return null
        return runCatching {
            val root = JSONObject(rawJson)
            val creatorName = root.optString("creatorName", "")
                .trim()
                .ifBlank { APP_META.DEFAULT_CREATOR_NAME }
            val creatorBio = root.optString("creatorBio", "")
                .trim()
                .ifBlank { APP_META.DEFAULT_CREATOR_BIO }
            val latestVersion = root.optString("latestVersion", "").trim()
            val updateUrl = root.optString("updateUrl", "").trim()
                .takeIf { isSafeExternalUrl(it) }
                .orEmpty()

            val linksList = mutableListOf<CreatorLink>()
            val rawLinks = root.opt("links")
            when (rawLinks) {
                is JSONArray -> {
                    for (i in 0 until rawLinks.length()) {
                        val obj = rawLinks.optJSONObject(i) ?: continue
                        val label = obj.optString("label", "").trim()
                            .ifBlank { obj.optString("name", "").trim() }
                            .ifBlank { obj.optString("title", "").trim() }
                        val url = obj.optString("url", "").trim()
                        if (label.isNotBlank() && url.isNotBlank() && isSafeExternalUrl(url)) {
                            linksList.add(CreatorLink(label = label, url = url))
                        }
                    }
                }
                is JSONObject -> {
                    val keys = rawLinks.keys()
                    while (keys.hasNext()) {
                        val key = keys.next().trim()
                        val url = rawLinks.optString(key, "").trim()
                        if (key.isNotBlank() && url.isNotBlank() && isSafeExternalUrl(url)) {
                            val prettyLabel = key.replaceFirstChar { it.uppercase() }
                            linksList.add(CreatorLink(label = prettyLabel, url = url))
                        }
                    }
                }
            }

            RemoteAppConfig(
                creatorName = creatorName,
                creatorBio = creatorBio,
                links = linksList,
                latestVersion = latestVersion,
                updateUrl = updateUrl
            )
        }.getOrNull()
    }
}
