package com.example.config

/**
 * Single top-level metadata & remote GitHub config definition for Epos.
 */
const val APP_NAME: String = "Epos"
const val APP_VERSION: String = "v1.0.0"
const val REMOTE_CONFIG_URL: String =
    "https://raw.githubusercontent.com/bfactuals/my-apps-config/refs/heads/main/epos-config.json"

data class CreatorLink(
    val label: String,
    val url: String
)

data class RemoteAppConfig(
    val creatorName: String = APP_META.DEFAULT_CREATOR_NAME,
    val creatorBio: String = APP_META.DEFAULT_CREATOR_BIO,
    val links: List<CreatorLink> = emptyList(),
    val latestVersion: String = "",
    val updateUrl: String = ""
) {
    val hasNewerUpdate: Boolean
        get() = updateUrl.isNotBlank() &&
            isSafeExternalUrl(updateUrl) &&
            isVersionNewer(latestVersion, APP_VERSION)
}

object APP_META {
    const val APP_TITLE: String = APP_NAME
    const val VERSION: String = APP_VERSION
    const val TAGLINE: String = "Quiet, tactile, offline-first reading."
    const val DEFAULT_CREATOR_NAME: String = "Bfactuals"
    const val DEFAULT_CREATOR_BIO: String =
        "Crafted by Bfactuals for distraction-free reading."

    val DEFAULT_REMOTE_CONFIG: RemoteAppConfig = RemoteAppConfig(
        creatorName = DEFAULT_CREATOR_NAME,
        creatorBio = DEFAULT_CREATOR_BIO,
        links = emptyList(),
        latestVersion = "",
        updateUrl = ""
    )
}

fun isSafeExternalUrl(url: String): Boolean {
    val clean = url.trim()
    return clean.startsWith("https://", ignoreCase = true) ||
        clean.startsWith("mailto:", ignoreCase = true)
}

fun isVersionNewer(latest: String, current: String): Boolean {
    val latestParts = parseSemVer(latest) ?: return false
    val currentParts = parseSemVer(current) ?: return false
    val maxLen = maxOf(latestParts.size, currentParts.size)
    for (i in 0 until maxLen) {
        val l = latestParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (l != c) return l > c
    }
    return false
}

private fun parseSemVer(raw: String): List<Int>? {
    val cleaned = raw.trim().removePrefix("v").removePrefix("V").substringBefore('-')
    if (cleaned.isBlank()) return null
    val parts = cleaned.split('.').mapNotNull { it.toIntOrNull() }
    return parts.takeIf { it.isNotEmpty() }
}
