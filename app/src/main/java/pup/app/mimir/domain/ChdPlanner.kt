package pup.app.mimir.domain

/** Builds a non-destructive CHD conversion plan. CHDMan keeps the source image intact. */
object ChdPlanner {
    fun buildPlan(
        entries: List<RomEntry>,
        system: ChdSystem,
        discType: ChdDiscType,
        deleteOriginalFiles: Boolean,
    ): OperationPlan {
        val changes = mutableListOf<PlannedChange>()
        val operations = mutableListOf<FileOperation>()
        val conflicts = mutableListOf<String>()
        val existingPaths = entries.associateBy { it.relativePath.lowercase() }
        val reservedPaths = mutableSetOf<String>()

        entries
            .filter { it.extension() in system.supportedExtensions }
            .sortedBy { it.relativePath.lowercase() }
            .forEach { entry ->
                val generatedTargetPath = "${entry.relativePath.substringBeforeLast('.', entry.relativePath)}.chd"
                val targetPath = existingPaths[generatedTargetPath.lowercase()]?.relativePath ?: generatedTargetPath
                if (!reservedPaths.add(targetPath)) {
                    conflicts += "Skipped ${entry.fileName}: another selected image would create $targetPath"
                    return@forEach
                }
                val operation = FileOperation.ConvertToChd(
                    sourcePath = entry.relativePath,
                    targetPath = targetPath,
                    system = system,
                    discType = discType,
                    deleteOriginalFiles = deleteOriginalFiles,
                )
                operations += operation
                changes += PlannedChange(
                    title = entry.fileName,
                    sourceFiles = listOf(entry.relativePath),
                    targetFiles = listOf(targetPath),
                    detailLabel = "${discType.displayName} CHD",
                    detailPath = targetPath,
                    operations = listOf(operation),
                    targetAlreadyExists = targetPath.lowercase() in existingPaths,
                )
            }

        return OperationPlan(
            mode = ToolMode.ChdConverter,
            changes = changes,
            operations = operations,
            conflicts = conflicts.distinct(),
        )
    }

    private fun RomEntry.extension(): String =
        fileName.substringAfterLast('.', "").lowercase()
}
