package pup.app.mimir.domain

object RomZipperPlanner {
    val supportedExtensions = setOf(
        // Nintendo handheld
        "gb",
        "gbc",
        "gba",
        "nds",

        // Nintendo home consoles
        "fds",
        "fig",
        "nes",
        "sfc",
        "smc",
        "swc",

        // Nintendo 64
        "n64",
        "v64",
        "z64",

        // Atari systems
        "a26",
        "a52",
        "a78",
        "lnx",

        // Sega home consoles
        "32x",
        "gen",
        "md",
        "smd",

        // Sega 8-bit and portable systems
        "gg",
        "sc",
        "sg",
        "sms",

        // NEC / other Japanese systems
        "ngc",
        "ngp",
        "pce",
        "sgx",
        "ws",
        "wsc",

        // Computer / miscellaneous formats
        "col",
        "cv",
        "d64",
        "mx1",
        "mx2",
        "rom",
        "sna",
        "tap",
        "tzx",
        "z80"
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
