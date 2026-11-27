package pup.app.mimir.domain

/**
 * A dotted version such as 1.2.0. Parts are compared as numbers, so 1.10.0 is newer than 1.9.0,
 * and a missing part counts as 0, so 1.2 equals 1.2.0.
 */
data class AppVersion(val parts: List<Int>) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        for (index in 0 until maxOf(parts.size, other.parts.size)) {
            val difference = parts.getOrElse(index) { 0 }.compareTo(other.parts.getOrElse(index) { 0 })
            if (difference != 0) return difference
        }
        return 0
    }

    override fun toString(): String = parts.joinToString(".")

    companion object {
        /** Reads "v1.2.0", "1.2" or "1.2.0-beta1" (the suffix is ignored); null when it is not a version. */
        fun parse(text: String): AppVersion? {
            val core = text.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore(' ')
            val parts = core.split('.').map { it.toIntOrNull() ?: return null }
            return AppVersion(parts)
        }
    }
}

/** The newest GitHub release, when it carries an APK Mimir can install. */
data class AvailableUpdate(
    val version: AppVersion,
    val title: String,
    val highlights: List<String>,
    val apkUrl: String,
    val apkSizeBytes: Long,
    /** The asset's SHA-256 as GitHub reports it, in lowercase hex, when it reports one. */
    val apkSha256: String?,
    val releasePageUrl: String,
)

object UpdatePolicy {
    /** Offer [latest] only when it is newer than the running app and the user has not skipped it. */
    fun shouldOffer(current: AppVersion?, latest: AppVersion, skippedVersion: String?): Boolean {
        if (current == null || latest <= current) return false
        return skippedVersion == null || AppVersion.parse(skippedVersion) != latest
    }
}

/** Turns a release's Markdown notes into the short "what's new" list shown in the update dialog. */
object ReleaseNotes {
    /** Sections that list pull requests or people rather than changes a user would notice. */
    private val skippedSections = setOf("what's changed", "new contributors")
    private val bold = Regex("""\*\*|__""")
    private val link = Regex("""\[([^\]]+)]\([^)]*\)""")
    private val issueRef = Regex("""\s*\((?:closes|fixes|fixed|resolves)\s+#\d+\)""", RegexOption.IGNORE_CASE)

    fun highlights(body: String): List<String> {
        var skipping = false
        return body.lineSequence()
            .map(String::trim)
            .mapNotNull { line ->
                when {
                    line.startsWith("#") -> {
                        val heading = line.trimStart('#').trim().lowercase().replace('’', '\'')
                        skipping = heading in skippedSections
                        null
                    }
                    skipping -> null
                    line.startsWith("* ") || line.startsWith("- ") -> clean(line.drop(2)).ifBlank { null }
                    else -> null
                }
            }
            .toList()
    }

    private fun clean(text: String): String =
        text.replace(bold, "").replace(link, "$1").replace(issueRef, "").trim()
}
