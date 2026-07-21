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
    ChdConverter("CHDMan"),
    VitaAppIds("Vita App IDs"),
}

enum class ChdDiscType(
    val displayName: String,
    val commandName: String,
) {
    Cd(displayName = "CD", commandName = "createcd"),
    Dvd(displayName = "DVD", commandName = "createdvd"),
}

enum class ChdSystem(
    val displayName: String,
    val supportedExtensions: Set<String>,
    val folderAliases: Set<String>,
) {
    Dreamcast(
        displayName = "Dreamcast",
        supportedExtensions = setOf("gdi", "cue", "iso"),
        folderAliases = setOf("dreamcast", "dc"),
    ),
    PlayStation1(
        displayName = "PS1",
        supportedExtensions = setOf("cue", "iso"),
        folderAliases = setOf("playstation 1", "ps1", "psx"),
    ),
    PlayStation2(
        displayName = "PS2",
        supportedExtensions = setOf("cue","iso"),
        folderAliases = setOf("playstation 2", "ps2"),
    ),
    SegaCd(
        displayName = "Sega CD",
        supportedExtensions = setOf("cue", "iso"),
        folderAliases = setOf("sega cd", "segacd", "mega cd", "megacd"),
    ),
    SegaSaturn(
        displayName = "Sega Saturn",
        supportedExtensions = setOf("cue", "iso"),
        folderAliases = setOf("sega saturn", "saturn"),
    ),
    PlayStationPortable(
        displayName = "PSP",
        supportedExtensions = setOf("iso"),
        folderAliases = setOf("playstation portable", "psp"),
    ),
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
    data class ConvertToChd(
        val sourcePath: String,
        val targetPath: String,
        val system: ChdSystem,
        val discType: ChdDiscType,
        val deleteOriginalFiles: Boolean,
    ) : FileOperation
}

data class PlannedChange(
    val title: String,
    val sourceFiles: List<String>,
    val targetFiles: List<String>,
    val detailLabel: String,
    val detailPath: String,
    val operations: List<FileOperation>,
    /** A matching output is already in the source folder and will be replaced if selected. */
    val targetAlreadyExists: Boolean = false,
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
