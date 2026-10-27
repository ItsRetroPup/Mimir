package pup.app.mimir.ui

import android.net.Uri
import pup.app.mimir.R
import pup.app.mimir.data.OperationCancellation
import pup.app.mimir.data.RomTreeRepository
import pup.app.mimir.domain.ChangePlanner
import pup.app.mimir.domain.ChdPlanner
import pup.app.mimir.domain.ConverterTool
import pup.app.mimir.domain.NativeConverterPlanner
import pup.app.mimir.domain.OperationPlan
import pup.app.mimir.domain.RomScanner
import pup.app.mimir.domain.RomZipperPlanner
import pup.app.mimir.domain.ToolMode

/** Scans the selected ROM folder the way the active tool needs and builds its preview plan. */
class ToolPlanBuilder(
    private val repository: RomTreeRepository,
    private val strings: AppStrings,
) {
    fun initialLabel(state: MimirUiState): String = when (state.selectedMode) {
        ToolMode.MultiDiscOrganizer -> strings.text(R.string.scan_start_multidisc)
        ToolMode.RomZipper -> strings.text(R.string.scan_start_zip)
        ToolMode.Converter -> strings.text(R.string.scan_start_tool, state.selectedConverterTool.displayName)
        ToolMode.VitaAppIds -> strings.text(R.string.scan_start_files)
        ToolMode.EsDeSystems -> ""
    }

    /** Shown when a scan finds nothing for the active tool. */
    fun emptyResultText(state: MimirUiState): String = when (state.selectedMode) {
        ToolMode.MultiDiscOrganizer -> strings.text(R.string.msg_none_multidisc)
        ToolMode.RomZipper -> strings.text(R.string.msg_none_zip)
        ToolMode.Converter -> strings.text(R.string.msg_none_converter, state.selectedConverterTool.displayName)
        ToolMode.VitaAppIds -> strings.text(R.string.msg_none_vita)
        ToolMode.EsDeSystems -> strings.text(R.string.msg_none_esde)
    }

    fun build(
        state: MimirUiState,
        romUri: Uri,
        cancellation: OperationCancellation,
        onProgressLabel: (String) -> Unit,
    ): OperationPlan {
        val onFileScanned: (Int) -> Unit = { scannedFiles ->
            if (scannedFiles % SCAN_PROGRESS_UPDATE_INTERVAL == 0) onProgressLabel(scannedLabel(state, scannedFiles))
        }
        val shouldStop = cancellation::shouldInterruptCurrentOperation
        return when (state.selectedMode) {
            ToolMode.MultiDiscOrganizer -> {
                val entries = repository.scanTree(
                    rootUri = romUri,
                    scanHiddenFolders = state.scanHiddenFolders,
                    onFileScanned = onFileScanned,
                    shouldStop = shouldStop,
                )
                onProgressLabel(strings.plural(R.plurals.scan_checking_multidisc, entries.size, entries.size))
                ChangePlanner.buildPlan(scanResult = RomScanner.scan(entries), preset = state.selectedPreset)
            }

            ToolMode.RomZipper -> {
                val entries = repository.scanTree(rootUri = romUri, onFileScanned = onFileScanned, shouldStop = shouldStop)
                onProgressLabel(strings.plural(R.plurals.scan_checking_zip, entries.size, entries.size))
                RomZipperPlanner.buildPlan(entries)
            }

            ToolMode.Converter -> if (state.selectedConverterTool == ConverterTool.Chd) {
                val system = state.selectedChdSystem
                val entries = repository.scanChdTree(
                    rootUri = romUri,
                    folderAliases = system.folderAliases,
                    supportedExtensions = system.supportedExtensions,
                    onFileScanned = onFileScanned,
                    shouldStop = shouldStop,
                )
                onProgressLabel(strings.plural(R.plurals.scan_checking_chd, entries.size, entries.size, system.displayName))
                ChdPlanner.buildPlan(
                    entries = entries,
                    system = system,
                    discType = state.selectedChdDiscType,
                    deleteOriginalFiles = state.deleteOriginalChdFiles,
                )
            } else {
                val tool = state.selectedConverterTool
                val scannedEntries = repository.scanChdTree(
                    rootUri = romUri,
                    folderAliases = tool.folderAliases,
                    supportedExtensions = tool.sourceExtensions,
                    outputExtension = tool.outputExtension,
                    onFileScanned = onFileScanned,
                    shouldStop = shouldStop,
                )
                onProgressLabel(strings.plural(R.plurals.scan_checking_tool, scannedEntries.size, scannedEntries.size, tool.displayName))
                val entries = if (tool == ConverterTool.XboxXiso) {
                    repository.filterXboxImagesNeedingXiso(romUri, scannedEntries, shouldStop)
                } else {
                    scannedEntries
                }
                NativeConverterPlanner.buildPlan(entries, tool)
            }

            ToolMode.VitaAppIds, ToolMode.EsDeSystems -> error("This tool does not use ROM scanning.")
        }
    }

    private fun scannedLabel(state: MimirUiState, scannedFiles: Int): String {
        val activity = when (state.selectedMode) {
            ToolMode.MultiDiscOrganizer -> strings.text(R.string.scan_activity_multidisc)
            ToolMode.RomZipper -> strings.text(R.string.scan_activity_zip)
            ToolMode.Converter -> strings.text(R.string.scan_activity_tool, state.selectedConverterTool.displayName)
            ToolMode.VitaAppIds -> strings.text(R.string.scan_activity_files)
            ToolMode.EsDeSystems -> ""
        }
        return strings.plural(R.plurals.scan_files_checked, scannedFiles, activity, scannedFiles)
    }

    private companion object {
        const val SCAN_PROGRESS_UPDATE_INTERVAL = 25
    }
}
