package pup.app.mimir.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupManifest(
    val sessionId: String,
    val sourceBackups: Map<String, String>,
    val createdFiles: List<String>,
    val createdDirectories: List<String>,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("sessionId", sessionId)
        .put("sourceBackups", JSONObject(sourceBackups))
        .put("createdFiles", JSONArray(createdFiles))
        .put("createdDirectories", JSONArray(createdDirectories))

    companion object {
        fun fromJson(json: JSONObject): BackupManifest =
            BackupManifest(
                sessionId = json.getString("sessionId"),
                sourceBackups = json.getJSONObject("sourceBackups").let { objectJson ->
                    objectJson.keys().asSequence().associateWith { key -> objectJson.getString(key) }
                },
                createdFiles = json.getJSONArray("createdFiles").toStringList(),
                createdDirectories = json.getJSONArray("createdDirectories").toStringList(),
            )

        private fun JSONArray.toStringList(): List<String> =
            buildList {
                for (index in 0 until length()) {
                    add(getString(index))
                }
            }
    }
}

class BackupManager(private val context: Context) {
    private val rootDir = File(context.filesDir, "backups")
    private val activeFile = File(rootDir, "active.json")

    init {
        rootDir.mkdirs()
    }

    fun sessionDir(sessionId: String): File =
        File(rootDir, sessionId).apply { mkdirs() }

    fun backupFile(sessionId: String, relativePath: String): File =
        File(sessionDir(sessionId), relativePath.replace('/', '_'))

    fun saveActiveManifest(manifest: BackupManifest) {
        activeFile.writeText(manifest.toJson().toString())
    }

    fun loadActiveManifest(): BackupManifest? =
        if (activeFile.exists()) BackupManifest.fromJson(JSONObject(activeFile.readText())) else null

    fun clearActiveManifest() {
        if (activeFile.exists()) {
            activeFile.delete()
        }
    }

    fun deleteSession(sessionId: String) {
        sessionDir(sessionId).deleteRecursively()
    }
}
