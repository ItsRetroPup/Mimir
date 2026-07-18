package pup.app.mimir.domain

object VitaAppIdPlanner {
    fun buildPlan(
        apps: List<VitaApp>,
        existingEntries: List<RomEntry> = emptyList(),
        format: VitaShortcutFormat = VitaShortcutFormat.Psvita,
    ): OperationPlan {
        val operations = mutableListOf<FileOperation>()
        val changes = mutableListOf<PlannedChange>()
        val conflicts = mutableListOf<String>()
        val existingPaths = existingEntries.map { it.relativePath }.toSet()
        val reservedPaths = mutableSetOf<String>()

        apps.sortedWith(compareBy({ it.title.lowercase() }, { it.titleId })).forEach { app ->
            val preferredFileName = "${sanitizeFileStem(app.title)}.${format.extension}"
            val fallbackFileName = "${sanitizeFileStem(app.title)} [${app.titleId}].${format.extension}"

            val targetPath = when {
                canReserve(preferredFileName, existingPaths, reservedPaths) ->
                    preferredFileName

                canReserve(fallbackFileName, existingPaths, reservedPaths) ->
                    fallbackFileName

                else -> {
                    conflicts += "Skipped ${app.titleId}: target already exists for ${app.title}"
                    return@forEach
                }
            }

            val operation = FileOperation.WriteTextFile(
                relativePath = targetPath,
                contents = format.fileContents(app.titleId),
            )
            operations += operation
            changes += PlannedChange(
                title = app.title,
                sourceFiles = listOf(
                    if (app.sourcePath == "shortcut-db") "Database match (${app.titleId})"
                    else "${app.sourcePath} (${app.titleId})"
                ),
                targetFiles = listOf(targetPath),
                detailLabel = "Create",
                detailPath = targetPath,
                operations = listOf(operation),
            )
        }

        return OperationPlan(
            mode = ToolMode.VitaAppIds,
            changes = changes,
            operations = operations,
            conflicts = conflicts.distinct(),
        )
    }

    private fun VitaShortcutFormat.fileContents(titleId: String): String =
        when (this) {
            VitaShortcutFormat.Psvita -> titleId
            VitaShortcutFormat.Dpt -> "[vita_game_id]$titleId"
        }

    private fun canReserve(
        path: String,
        existingPaths: Set<String>,
        reservedPaths: MutableSet<String>,
    ): Boolean {
        if (path in existingPaths) return false
        return reservedPaths.add(path)
    }

    private fun sanitizeFileStem(title: String): String {
        val cleaned = buildString(title.length) {
            title.forEach { char ->
                append(
                    when (char) {
                        '<', '>', ':', '"', '/', '\\', '|', '?', '*' -> ' '
                        else -> char
                    }
                )
            }
        }
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.')

        return cleaned.ifBlank { "Unknown Vita Title" }
    }
}
