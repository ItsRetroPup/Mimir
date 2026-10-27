package pup.app.mimir.data

import androidx.documentfile.provider.DocumentFile

/** Resolves a `/`-separated path below this folder, or null when any segment is missing. */
fun DocumentFile.findRelative(relativePath: String): DocumentFile? {
    var current = this
    val parts = relativePath.split('/').filter(String::isNotBlank)
    parts.forEachIndexed { index, part ->
        val child = current.findFile(part) ?: return null
        if (index == parts.lastIndex) return child
        current = child
    }
    return null
}
