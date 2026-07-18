package pup.app.mimir.domain

object ChangePlanner {
    fun buildPlan(scanResult: ScanResult, preset: FrontendPreset): OperationPlan {
        val operations = mutableListOf<FileOperation>()
        val changes = mutableListOf<PlannedChange>()
        val conflicts = mutableListOf<String>()
        val existingPaths = scanResult.allEntries
            .map { it.relativePath }
            .toMutableSet()
        val reservedPaths = mutableSetOf<String>()

        scanResult.discSets.forEach { discSet ->
            val plan = when {
                discSet.parentPath.split('/').any { it.equals("ps2", ignoreCase = true) } ->
                    buildPs2Plan(discSet)
                else -> when (preset.layout) {
                    FrontendLayout.FolderAsFile -> buildFolderAsFilePlan(discSet)
                    FrontendLayout.PlaylistAtRootWithoutMoves -> buildRootPlaylistWithoutMovesPlan(discSet)
                    FrontendLayout.PlaylistAtRootWithDiscFolder -> buildRootPlaylistPlan(discSet)
                }
            }

            val discSetConflicts = mutableListOf<String>()
            val allTargets = plan.createdDirectories + plan.createdFiles
            allTargets.forEach { path ->
                if (path in existingPaths || !reservedPaths.add(path)) {
                    discSetConflicts += "Skipped ${discSet.title}: target already exists: $path"
                }
            }

            if (discSetConflicts.isNotEmpty()) {
                conflicts += discSetConflicts
                return@forEach
            }

            val changeOperations = buildList {
                addAll(plan.createdDirectories.map(FileOperation::CreateDirectory))
                addAll(plan.moves.map { FileOperation.MoveFile(it.first, it.second) })
                if (plan.playlistPath != null) {
                    add(FileOperation.WriteTextFile(plan.playlistPath, plan.playlistContents.orEmpty()))
                }
            }
            operations += changeOperations
            changes += PlannedChange(
                title = discSet.title,
                sourceFiles = discSet.entries.map { it.relativePath },
                targetFiles = plan.targetFiles,
                detailLabel = if (plan.playlistPath == null) "Disc folder" else "Playlist",
                detailPath = plan.playlistPath ?: plan.createdDirectories.single(),
                operations = changeOperations,
            )
        }

        return OperationPlan(
            mode = ToolMode.MultiDiscOrganizer,
            preset = preset,
            changes = changes,
            operations = operations,
            conflicts = conflicts.distinct(),
        )
    }

    private fun buildFolderAsFilePlan(discSet: DiscGameSet): InternalPlan {
        val folderName = "${discSet.title}.m3u"
        val containerPath = RelativePaths.join(discSet.parentPath, folderName)
        val playlistPath = RelativePaths.join(containerPath, "${discSet.title}.m3u")
        val moves = discSet.entries.map { entry ->
            entry.sourcePath to RelativePaths.join(containerPath, entry.fileName)
        }
        val playlistContents = moves.joinToString("\n") { (_, target) ->
            RelativePaths.nameOf(target)
        }
        return InternalPlan(
            createdDirectories = listOf(containerPath),
            moves = moves,
            targetFiles = moves.map { it.second },
            createdFiles = moves.map { it.second } + playlistPath,
            playlistPath = playlistPath,
            playlistContents = playlistContents,
        )
    }

    private fun buildRootPlaylistPlan(discSet: DiscGameSet): InternalPlan {
        val discFolder = RelativePaths.join(discSet.parentPath, discSet.title)
        val playlistPath = RelativePaths.join(discSet.parentPath, "${discSet.title}.m3u")
        val moves = discSet.entries.map { entry ->
            entry.sourcePath to RelativePaths.join(discFolder, entry.fileName)
        }
        val playlistContents = moves.joinToString("\n") { (_, target) ->
            val folderName = RelativePaths.nameOf(discFolder)
            "$folderName/${RelativePaths.nameOf(target)}"
        }
        return InternalPlan(
            createdDirectories = listOf(discFolder),
            moves = moves,
            targetFiles = moves.map { it.second },
            createdFiles = moves.map { it.second } + playlistPath,
            playlistPath = playlistPath,
            playlistContents = playlistContents,
        )
    }

    private fun buildRootPlaylistWithoutMovesPlan(discSet: DiscGameSet): InternalPlan {
        val playlistPath = RelativePaths.join(discSet.parentPath, "${discSet.title}.m3u")
        val playlistContents = discSet.entries.joinToString("\n") { entry ->
            entry.fileName
        }
        return InternalPlan(
            createdDirectories = emptyList(),
            moves = emptyList(),
            targetFiles = discSet.entries.map { it.relativePath },
            createdFiles = listOf(playlistPath),
            playlistPath = playlistPath,
            playlistContents = playlistContents,
        )
    }

    private fun buildPs2Plan(discSet: DiscGameSet): InternalPlan {
        val discFolder = RelativePaths.join(discSet.parentPath, discSet.title)
        val moves = discSet.entries.map { entry ->
            entry.sourcePath to RelativePaths.join(discFolder, entry.fileName)
        }
        return InternalPlan(
            createdDirectories = listOf(discFolder),
            moves = moves,
            targetFiles = moves.map { it.second },
            createdFiles = moves.map { it.second },
            playlistPath = null,
            playlistContents = null,
        )
    }

    private data class InternalPlan(
        val createdDirectories: List<String>,
        val moves: List<Pair<String, String>>,
        val targetFiles: List<String>,
        val createdFiles: List<String>,
        val playlistPath: String?,
        val playlistContents: String?,
    )
}
