package pup.app.mimir.domain

/** Builds a non-destructive CHD conversion plan. CHDMan keeps the source image intact. */
object ChdPlanner {
    /**
     * Plans every system found in one scan. [ps2DiscType] is the only choice the user makes:
     * PS2 games convert as CD for NetherSX2 or DVD for ARMSX2; every other system has one right answer.
     */
    fun buildPlan(
        entriesBySystem: Map<ChdSystem, List<RomEntry>>,
        ps2DiscType: ChdDiscType,
        deleteOriginalFiles: Boolean,
    ): OperationPlan {
        val changes = mutableListOf<PlannedChange>()
        val conflicts = mutableListOf<String>()
        val existingPaths = entriesBySystem.values.flatten().associateBy { it.relativePath.lowercase() }
        val reservedPaths = mutableSetOf<String>()

        ChdSystem.entries.forEach { system ->
            val discType = system.discType(ps2DiscType)
            entriesBySystem[system].orEmpty()
                .filter { it.extension() in system.supportedExtensions }
                .sortedBy { it.relativePath.lowercase() }
                .forEach { entry ->
                    val generatedTargetPath = "${entry.relativePath.substringBeforeLast('.', entry.relativePath)}.chd"
                    val targetPath = existingPaths[generatedTargetPath.lowercase()]?.relativePath ?: generatedTargetPath
                    if (!reservedPaths.add(targetPath.lowercase())) {
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
                    changes += PlannedChange(
                        title = entry.fileName,
                        sourceFiles = listOf(entry.relativePath),
                        targetFiles = listOf(targetPath),
                        detailLabel = system.displayName,
                        detailPath = targetPath,
                        operations = listOf(operation),
                        sourceSizeBytes = entry.sizeBytes,
                        targetAlreadyExists = targetPath.lowercase() in existingPaths,
                    )
                }
        }

        return OperationPlan(
            mode = ToolMode.Converter,
            changes = changes,
            operations = changes.flatMap(PlannedChange::operations),
            conflicts = conflicts.distinct(),
        )
    }

    /** Plans a single system, as when its folder is the only one scanned. */
    fun buildPlan(
        entries: List<RomEntry>,
        system: ChdSystem,
        discType: ChdDiscType,
        deleteOriginalFiles: Boolean,
    ): OperationPlan = buildPlan(
        entriesBySystem = mapOf(system to entries),
        ps2DiscType = if (system == ChdSystem.PlayStation2) discType else ChdDiscType.Cd,
        deleteOriginalFiles = deleteOriginalFiles,
    )

    /** Switches every queued PS2 conversion to [ps2DiscType], chosen in the convert dialog. */
    fun withPs2DiscType(operation: FileOperation, ps2DiscType: ChdDiscType): FileOperation =
        if (operation is FileOperation.ConvertToChd && operation.system == ChdSystem.PlayStation2) {
            operation.copy(discType = ps2DiscType)
        } else {
            operation
        }

    private fun RomEntry.extension(): String =
        fileName.substringAfterLast('.', "").lowercase()
}
