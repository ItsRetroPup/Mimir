package com.mimir.companion.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mimir.companion.domain.RomEntry

class RomTreeRepository(private val context: Context) {
    fun scanTree(rootUri: Uri): List<RomEntry> {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
        return buildList { walk(root, emptyList(), this) }
    }

    private fun walk(
        node: DocumentFile,
        segments: List<String>,
        collector: MutableList<RomEntry>,
    ) {
        node.listFiles()
            .sortedBy { it.name.orEmpty().lowercase() }
            .forEach { child ->
                val childName = child.name ?: return@forEach
                if (child.isDirectory) {
                    walk(child, segments + childName, collector)
                } else if (child.isFile) {
                    collector += RomEntry(
                        relativePath = (segments + childName).joinToString("/"),
                        fileName = childName,
                    )
                }
            }
    }
}
