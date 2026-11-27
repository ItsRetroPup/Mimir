package pup.app.mimir.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pup.app.mimir.domain.AppVersion
import pup.app.mimir.domain.AvailableUpdate
import pup.app.mimir.domain.ReleaseNotes

/**
 * Checks GitHub Releases for a newer Mimir, downloads its APK into the app's cache and hands it
 * to Android's package installer.
 */
class UpdateRepository(private val context: Context) {

    /** The version name of the installed app, such as 1.1.0. */
    fun installedVersion(): AppVersion? =
        packageInfo(context.packageName, 0)?.versionName?.let(AppVersion::parse)

    /** True for debug builds, which are signed with the debug key and so can't update to a release. */
    fun isDebugBuild(): Boolean =
        context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

    /** Reads the latest published release; null when it has no APK attached. */
    suspend fun fetchLatest(): AvailableUpdate? = withContext(Dispatchers.IO) {
        val json = JSONObject(get(LATEST_RELEASE_URL).decodeToString())
        val version = AppVersion.parse(json.getString("tag_name")) ?: return@withContext null
        val assets = json.optJSONArray("assets") ?: return@withContext null
        val apk = (0 until assets.length())
            .map(assets::getJSONObject)
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?: return@withContext null
        AvailableUpdate(
            version = version,
            title = json.optString("name").ifBlank { "Mimir $version" },
            highlights = ReleaseNotes.highlights(json.optString("body")),
            apkUrl = apk.getString("browser_download_url"),
            apkSizeBytes = apk.optLong("size"),
            // GitHub reports it as "sha256:<hex>" on newer uploads.
            apkSha256 = apk.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")?.lowercase(),
            releasePageUrl = json.optString("html_url"),
        )
    }

    /**
     * Downloads the update's APK and checks it arrived whole. [onProgress] gets 0..1, or null when
     * the size is unknown. Throws when the download fails or the file doesn't match GitHub's checksum.
     */
    suspend fun download(update: AvailableUpdate, onProgress: (Float?) -> Unit): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }
        // Only one update is ever kept, so old downloads don't pile up in the cache.
        directory.listFiles()?.forEach(File::delete)
        val partial = File(directory, "Mimir-${update.version}.apk.part")
        val target = File(directory, "Mimir-${update.version}.apk")
        val digest = MessageDigest.getInstance("SHA-256")

        val connection = open(update.apkUrl)
        try {
            check(connection.responseCode in 200..299) { "GitHub returned HTTP ${connection.responseCode}." }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: update.apkSizeBytes.takeIf { it > 0 }
            var copied = 0L
            var lastReported = -1
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        copied += read
                        val percent = total?.let { (copied * 100 / it).toInt() }
                        // Report each whole percent once, so the UI isn't flooded with updates.
                        if (percent == null || percent != lastReported) {
                            lastReported = percent ?: -1
                            onProgress(total?.let { (copied.toFloat() / it).coerceIn(0f, 1f) })
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        update.apkSha256?.let { expected ->
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual != expected) {
                partial.delete()
                error("The download was damaged. Try again.")
            }
        }
        check(partial.renameTo(target)) { "Couldn't save the update." }
        target
    }

    /**
     * True when [apk] is Mimir and is signed with the same key as the installed app. Android
     * refuses an update signed with another key, so this catches it before the installer opens.
     */
    fun isSignedLikeInstalledApp(apk: File): Boolean {
        val downloaded = archiveInfo(apk) ?: return false
        val installed = packageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES) ?: return false
        if (downloaded.packageName != context.packageName) return false
        val downloadedSigners = downloaded.signingInfo?.apkContentsSigners?.toSet() ?: return false
        val installedSigners = installed.signingInfo?.apkContentsSigners?.toSet() ?: return false
        return downloadedSigners.isNotEmpty() && downloadedSigners == installedSigners
    }

    /** Opens Android's installer for [apk]. Android asks once to allow installs from Mimir. */
    fun launchInstaller(apk: File) {
        // A content:// link that lets the installer read this one file from Mimir's private cache.
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun get(url: String): ByteArray {
        val connection = open(url).apply { setRequestProperty("Accept", "application/vnd.github+json") }
        return try {
            check(connection.responseCode in 200..299) { "GitHub returned HTTP ${connection.responseCode}." }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            // GitHub's release downloads redirect to its file host; both are https, so this is followed.
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mimir-updater")
        }

    @Suppress("DEPRECATION")
    private fun packageInfo(packageName: String, flags: Int): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            context.packageManager.getPackageInfo(packageName, flags)
        }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun archiveInfo(apk: File): PackageInfo? {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(apk.path, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            context.packageManager.getPackageArchiveInfo(apk.path, flags)
        }
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/ItsRetroPup/Mimir/releases/latest"
        const val UPDATE_DIR = "updates"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
