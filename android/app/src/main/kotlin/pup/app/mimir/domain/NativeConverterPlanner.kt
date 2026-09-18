package pup.app.mimir.domain

/** Builds a non-destructive plan for a single-file native converter. */
object NativeConverterPlanner {
    fun buildPlan(entries: List<RomEntry>, tool: ConverterTool): OperationPlan {
        require(tool != ConverterTool.Chd) { "CHD conversions use ChdPlanner." }
        val existingPaths = entries.associateBy { it.relativePath.lowercase() }
        val reservedPaths = mutableSetOf<String>()
        val conflicts = mutableListOf<String>()
        val changes = mutableListOf<PlannedChange>()

        entries
            .filter { it.extension() in tool.sourceExtensions }
            .sortedBy { it.relativePath.lowercase() }
            .forEach { entry ->
                val generatedTarget = "${entry.relativePath.substringBeforeLast('.', entry.relativePath)}.${tool.outputExtension}"
                val targetPath = existingPaths[generatedTarget.lowercase()]?.relativePath ?: generatedTarget
                if (!reservedPaths.add(targetPath.lowercase())) {
                    conflicts += "Skipped ${entry.fileName}: another selected image would create $targetPath"
                    return@forEach
                }
                val operation = FileOperation.ConvertWithTool(entry.relativePath, targetPath, tool)
                changes += PlannedChange(
                    title = entry.fileName,
                    sourceFiles = listOf(entry.relativePath),
                    targetFiles = listOf(targetPath),
                    detailLabel = tool.displayName,
                    detailPath = targetPath,
                    operations = listOf(operation),
                    sourceSizeBytes = entry.sizeBytes,
                    targetAlreadyExists = targetPath.lowercase() in existingPaths,
                )
            }
        return OperationPlan(
            mode = ToolMode.ChdConverter,
            changes = changes,
            operations = changes.flatMap(PlannedChange::operations),
            conflicts = conflicts.distinct(),
        )
    }

    private fun RomEntry.extension(): String = fileName.substringAfterLast('.', "").lowercase()
}
