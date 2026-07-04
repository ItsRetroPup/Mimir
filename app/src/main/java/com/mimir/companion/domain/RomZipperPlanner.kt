package com.mimir.companion.domain

object RomZipperPlanner {
    val supportedExtensions = setOf(
        "nds",
        "gb",
        "gbc",
        "gba",
        "nes",
        "fds",
        "sfc",
        "smc",
        "z64",
        "n64",
        "v64",
        "a26",
        "sms",
        "gg",
        "md",
        "gen",
    )

    fun buildPlan(entries: List<RomEntry>): OperationPlan {
        val changes = mutableListOf<PlannedChange>()
        val operations = mutableListOf<FileOperation>()
        val conflicts = mutableListOf<String>()
        val existingPaths = entries.map { it.relativePath }.toSet()
        val reservedPaths = mutableSetOf<String>()

        entries
            .filter { it.extension() in supportedExtensions }
            .sortedBy { it.relativePath.lowercase() }
            .forEach { entry ->
                val stemPath = entry.relativePath.substringBeforeLast('.', entry.relativePath)
                val targetPath = "$stemPath.zip"
                if (targetPath in existingPaths || !reservedPaths.add(targetPath)) {
                    conflicts += "Skipped ${entry.fileName}: target already exists: $targetPath"
                    return@forEach
                }
                operations += FileOperation.ZipFile(
                    sourcePath = entry.relativePath,
                    targetPath = targetPath,
                    archiveEntryName = entry.fileName,
                )
                changes += PlannedChange(
                    title = entry.fileName,
                    sourceFiles = listOf(entry.relativePath),
                    targetFiles = listOf(targetPath),
                    detailLabel = "Zip",
                    detailPath = targetPath,
                )
            }

        return OperationPlan(
            mode = ToolMode.RomZipper,
            changes = changes,
            operations = operations,
            conflicts = conflicts.distinct(),
        )
    }

    private fun RomEntry.extension(): String =
        fileName.substringAfterLast('.', "").lowercase()
}
