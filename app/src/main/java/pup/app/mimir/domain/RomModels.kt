package pup.app.mimir.domain

data class RomEntry(
    val relativePath: String,
    val fileName: String,
    val sourcePath: String = relativePath,
)

data class VitaApp(
    val titleId: String,
    val title: String,
    val sourcePath: String,
)

enum class VitaShortcutFormat(
    val extension: String,
    val displayName: String,
) {
    Psvita(extension = "psvita", displayName = ".psvita"),
    Dpt(extension = "dpt", displayName = ".dpt"),
}

enum class ToolMode(val displayName: String) {
    MultiDiscOrganizer("Multi-disc Organizer"),
    RomZipper("RomZipper"),
    VitaAppIds("Vita App IDs"),
}

data class DiscMatch(
    val title: String,
    val discNumber: Int,
)

data class DiscGameSet(
    val title: String,
    val parentPath: String,
    val entries: List<RomEntry>,
)

data class ScanResult(
    val totalFiles: Int,
    val allEntries: List<RomEntry>,
    val discSets: List<DiscGameSet>,
)

sealed interface FileOperation {
    data class CreateDirectory(val relativePath: String) : FileOperation
    data class MoveFile(val sourcePath: String, val targetPath: String) : FileOperation
    data class WriteTextFile(val relativePath: String, val contents: String) : FileOperation
    data class ZipFile(
        val sourcePath: String,
        val targetPath: String,
        val archiveEntryName: String,
    ) : FileOperation
}

data class PlannedChange(
    val title: String,
    val sourceFiles: List<String>,
    val targetFiles: List<String>,
    val detailLabel: String,
    val detailPath: String,
    val operations: List<FileOperation>,
)

data class OperationPlan(
    val mode: ToolMode,
    val preset: FrontendPreset? = null,
    val changes: List<PlannedChange>,
    val operations: List<FileOperation>,
    val conflicts: List<String>,
) {
    fun forSelectedChanges(selectedDetailPaths: Set<String>): OperationPlan {
        val selectedChanges = changes.filter { it.detailPath in selectedDetailPaths }
        return copy(
            changes = selectedChanges,
            operations = selectedChanges.flatMap(PlannedChange::operations),
        )
    }
}
