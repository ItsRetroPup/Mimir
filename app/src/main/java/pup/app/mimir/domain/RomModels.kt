package pup.app.mimir.domain

data class RomEntry(
    val relativePath: String,
    val fileName: String,
    val sourcePath: String = relativePath,
    val sizeBytes: Long = 0L,
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
    Converter("Converter Tools"),
    VitaAppIds("Vita App IDs"),
    EsDeSystems("ES-DE Systems"),
}

/** How a converter runs on the device. */
sealed interface ConverterBackend {
    /** A bundled ARM64 executable in the app's native library directory. */
    data class NativeExecutable(val fileName: String) : ConverterBackend

    /** The NSZ Python package running in the embedded Chaquopy runtime. */
    data object EmbeddedPython : ConverterBackend

    /** Kotlin code that streams the source and replaces it under the same name. */
    data object InPlaceKotlin : ConverterBackend
}

enum class ConverterTool(
    val displayName: String,
    val sourceExtensions: Set<String>,
    val outputExtension: String,
    val folderAliases: Set<String>,
    val backend: ConverterBackend,
    /** Short output label shown on queued conversions. */
    val badge: String,
) {
    Chd(
        displayName = "CHDMan",
        sourceExtensions = emptySet(),
        outputExtension = "chd",
        folderAliases = emptySet(),
        backend = ConverterBackend.NativeExecutable("libchdman.so"),
        badge = "CHD",
    ),
    DolphinRvz(
        displayName = "Dolphin RVZ",
        sourceExtensions = setOf("iso"),
        outputExtension = "rvz",
        folderAliases = setOf("gamecube", "gc", "nintendo gamecube", "wii", "nintendo wii"),
        backend = ConverterBackend.NativeExecutable("libdolphintool.so"),
        badge = "RVZ",
    ),
    AzaharZcci(
        displayName = "Azahar ZCCI",
        sourceExtensions = setOf("3ds", "cci"),
        outputExtension = "zcci",
        folderAliases = setOf("3ds", "nintendo 3ds"),
        backend = ConverterBackend.NativeExecutable("libazahar.so"),
        badge = "ZCCI",
    ),
    NszNsp(
        displayName = "NSZ to NSP",
        sourceExtensions = setOf("nsz"),
        outputExtension = "nsp",
        folderAliases = setOf("switch", "nintendo switch"),
        backend = ConverterBackend.EmbeddedPython,
        badge = "NSP",
    ),
    XboxXiso(
        displayName = "Xbox XISO",
        sourceExtensions = setOf("iso"),
        outputExtension = "iso",
        folderAliases = setOf("xbox", "microsoft xbox"),
        backend = ConverterBackend.InPlaceKotlin,
        badge = "XISO",
    ),
    ;

    /** True when the converted image replaces its source under the same name. */
    val replacesSource: Boolean get() = backend == ConverterBackend.InPlaceKotlin
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
        supportedExtensions = setOf("iso"),
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
    data class ConvertWithTool(
        val sourcePath: String,
        val targetPath: String,
        val tool: ConverterTool,
    ) : FileOperation
}

data class PlannedChange(
    val title: String,
    val sourceFiles: List<String>,
    val targetFiles: List<String>,
    val detailLabel: String,
    val detailPath: String,
    val operations: List<FileOperation>,
    val sourceSizeBytes: Long = 0L,
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
