package pup.app.mimir.ui

import pup.app.mimir.data.RomTreeRepository
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import pup.app.mimir.R
import pup.app.mimir.data.StopRequest
import pup.app.mimir.domain.ChdDiscType
import pup.app.mimir.domain.ChdSystem
import pup.app.mimir.domain.ConverterTool
import pup.app.mimir.domain.FrontendPreset
import pup.app.mimir.domain.ToolMode
import pup.app.mimir.domain.VitaApp
import pup.app.mimir.domain.VitaShortcutFormat

@Composable
internal fun MimirScreen(
    uiState: MimirUiState,
    onSelectFolder: () -> Unit,
    onSelectVitaOutput: () -> Unit,
    onSelectNszKeys: () -> Unit,
    onSelectEsDeFolder: () -> Unit,
    onRefreshEsDeSystems: () -> Unit,
    onApplyEsDeSystems: () -> Unit,
    onPickEsDeSystemFolder: (String) -> Unit,
    onModeSelected: (ToolMode) -> Unit,
    onPresetSelected: (FrontendPreset) -> Unit,
    onChdSystemSelected: (ChdSystem) -> Unit,
    onChdDiscTypeSelected: (ChdDiscType) -> Unit,
    onConverterToolSelected: (ConverterTool) -> Unit,
    onDeleteOriginalChdFilesChanged: (Boolean) -> Unit,
    onScanHiddenFoldersChanged: (Boolean) -> Unit,
    onDarkModeToggled: (Boolean) -> Unit,
    onVitaQueryChanged: (String) -> Unit,
    onVitaShortcutFormatSelected: (VitaShortcutFormat) -> Unit,
    onVitaShortcutAdd: (VitaApp) -> Unit,
    onVitaShortcutRemove: (VitaApp) -> Unit,
    onChangeSelection: (String, Boolean) -> Unit,
    onSelectAllChanges: () -> Unit,
    onDeselectAllChanges: () -> Unit,
    onStart: () -> Unit,
    onApply: () -> Unit,
    onStopNow: () -> Unit,
    onStopAfterCurrent: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalMimirPalette.current
    var currentSection by rememberSaveable { mutableStateOf(AppSection.Home) }
    var showApplyConfirmation by rememberSaveable { mutableStateOf(false) }
    var showEsDeConfirmation by rememberSaveable { mutableStateOf(false) }
    var pendingVitaAdd by remember { mutableStateOf<VitaApp?>(null) }
    var pendingVitaRemove by remember { mutableStateOf<VitaApp?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var queueSortOption by rememberSaveable { mutableStateOf(QueueSortOption.NameAscending.name) }
    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
    val selectTool: (ToolEntry) -> Unit = { tool ->
        onModeSelected(tool.mode)
        tool.converter?.let(onConverterToolSelected)
        currentSection = tool.section
    }
    val goHome = { currentSection = AppSection.Home }
    BackHandler(enabled = currentSection != AppSection.Home, onBack = goHome)

    // With a controller or keyboard, focus starts on the screen's first control, or on the save
    // block you came back from; touch input never shows focus.
    val contentFocus = remember { FocusRequester() }
    val returnFocus = remember { FocusRequester() }
    var lastToolSection by rememberSaveable { mutableStateOf<AppSection?>(null) }
    val inputModeManager = LocalInputModeManager.current
    LaunchedEffect(currentSection) {
        snapshotFlow { inputModeManager.inputMode }.first { it == InputMode.Keyboard }
        val returning = currentSection == AppSection.Home && lastToolSection != null
        if (returning) returnFocus.requestFocus() else contentFocus.requestFocus()
        if (currentSection != AppSection.Home) lastToolSection = currentSection
    }

    val tool = toolFor(currentSection)
    val ink = tool?.ink?.resolve(palette) ?: palette.edge
    val legend = legendFor(
        tool = tool,
        uiState = uiState,
        onHome = goHome,
        onToggleTheme = { onDarkModeToggled(!uiState.useDarkMode) },
        onScan = onStart,
        onApply = { showApplyConfirmation = true },
        onDownloadEsDe = onRefreshEsDeSystems,
        onInstallEsDe = { showEsDeConfirmation = true },
    )
    // X and Y run the legend's actions, as a console's face buttons would.
    val faceButtons = Modifier.onPreviewKeyEvent { event ->
        val letter = when (event.key) {
            Key.ButtonX -> "X"
            Key.ButtonY -> "Y"
            else -> return@onPreviewKeyEvent false
        }
        val action = legend.firstOrNull { it.letter == letter && it.onClick != null } ?: return@onPreviewKeyEvent false
        if (event.type == KeyEventType.KeyUp && action.enabled) action.onClick?.invoke()
        true
    }

    InkTheme(ink) {
        Box(
            Modifier
                .fillMaxSize()
                .consoleField(palette)
                .then(faceButtons),
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    HeaderStrip(
                        tool = tool,
                        uiState = uiState,
                        contentFocus = contentFocus,
                        onHome = goHome,
                        onToggleTheme = { onDarkModeToggled(!uiState.useDarkMode) },
                    )
                },
                bottomBar = {
                    LegendBar(legend) {
                        if (tool == null) AboutLink(onClick = { showAbout = true })
                    }
                },
            ) { innerPadding ->
                if (tool == null) {
                    HomeScreen(
                        contentFocus = contentFocus,
                        returnSection = lastToolSection,
                        returnFocus = returnFocus,
                        onOpen = selectTool,
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    ToolScreen(
                        tool = tool,
                        uiState = uiState,
                        contentFocus = contentFocus,
                        queueSortOption = QueueSortOption.valueOf(queueSortOption),
                        onQueueSortOptionChanged = { queueSortOption = it.name },
                        onSelectFolder = onSelectFolder,
                        onSelectVitaOutput = onSelectVitaOutput,
                        onSelectNszKeys = onSelectNszKeys,
                        onSelectEsDeFolder = onSelectEsDeFolder,
                        onPickEsDeSystemFolder = onPickEsDeSystemFolder,
                        onPresetSelected = onPresetSelected,
                        onChdSystemSelected = onChdSystemSelected,
                        onChdDiscTypeSelected = onChdDiscTypeSelected,
                        onScanHiddenFoldersChanged = onScanHiddenFoldersChanged,
                        onVitaQueryChanged = onVitaQueryChanged,
                        onVitaShortcutFormatSelected = onVitaShortcutFormatSelected,
                        onVitaShortcutAdd = { pendingVitaAdd = it },
                        onVitaShortcutRemove = { pendingVitaRemove = it },
                        onChangeSelection = onChangeSelection,
                        onSelectAllChanges = onSelectAllChanges,
                        onDeselectAllChanges = onDeselectAllChanges,
                        onStopNow = onStopNow,
                        onStopAfterCurrent = onStopAfterCurrent,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }

        if (showApplyConfirmation) {
            val isChdConversion = uiState.selectedMode == ToolMode.Converter &&
                uiState.selectedConverterTool == ConverterTool.Chd
            ApplyConfirmationDialog(
                mode = uiState.selectedMode,
                converterTool = uiState.selectedConverterTool,
                changeCount = uiState.selectedChangePaths.size,
                existingOutputCount = uiState.previewPlan?.changes
                    ?.count { it.targetAlreadyExists && it.detailPath in uiState.selectedChangePaths }
                    ?: 0,
                deleteOriginalChdFiles = uiState.deleteOriginalChdFiles,
                enabled = !uiState.isBusy,
                onDismiss = { showApplyConfirmation = false },
                onConfirm = { deleteOriginals ->
                    if (isChdConversion) onDeleteOriginalChdFilesChanged(deleteOriginals)
                    showApplyConfirmation = false
                    onApply()
                },
            )
        }

        if (showEsDeConfirmation) {
            EsDeInstallDialog(
                systemCount = uiState.esDeSystems.size,
                onDismiss = { showEsDeConfirmation = false },
                onConfirm = {
                    showEsDeConfirmation = false
                    onApplyEsDeSystems()
                },
            )
        }

        pendingVitaAdd?.let { app ->
            VitaShortcutAddDialog(
                app = app,
                format = uiState.vitaShortcutFormat,
                onDismiss = { pendingVitaAdd = null },
                onConfirm = {
                    pendingVitaAdd = null
                    onVitaShortcutAdd(app)
                },
            )
        }

        if (showAbout) {
            AboutDialog(onOpenUrl = openUrl, onDismiss = { showAbout = false })
        }

        pendingVitaRemove?.let { app ->
            VitaShortcutRemoveDialog(
                app = app,
                onDismiss = { pendingVitaRemove = null },
                onConfirm = {
                    pendingVitaRemove = null
                    onVitaShortcutRemove(app)
                },
            )
        }
    }
}

/** What the face buttons do on the current screen; the legend bar draws exactly this list. */
@Composable
private fun legendFor(
    tool: ToolEntry?,
    uiState: MimirUiState,
    onHome: () -> Unit,
    onToggleTheme: () -> Unit,
    onScan: () -> Unit,
    onApply: () -> Unit,
    onDownloadEsDe: () -> Unit,
    onInstallEsDe: () -> Unit,
): List<LegendAction> {
    if (tool == null) {
        return listOf(
            LegendAction("A", stringResource(R.string.legend_open)),
            LegendAction("Y", stringResource(R.string.legend_theme), onClick = onToggleTheme),
        )
    }
    val idle = !uiState.isBusy
    val selected = uiState.selectedChangePaths.size
    return buildList {
        add(LegendAction("A", stringResource(R.string.legend_select)))
        add(LegendAction("B", stringResource(R.string.legend_back), onClick = onHome))
        when (tool.mode) {
            ToolMode.VitaAppIds -> Unit
            ToolMode.EsDeSystems -> {
                add(LegendAction("X", stringResource(R.string.legend_download), enabled = idle && uiState.esDeRootUri != null, onClick = onDownloadEsDe))
                add(
                    LegendAction(
                        "Y",
                        stringResource(R.string.legend_install),
                        enabled = idle && uiState.esDeSystems.isNotEmpty(),
                        emphasised = uiState.esDeSystems.isNotEmpty(),
                        onClick = onInstallEsDe,
                    ),
                )
            }
            else -> {
                val keysReady = tool.converter != ConverterTool.NszNsp || uiState.nszKeysConfigured
                add(
                    LegendAction(
                        "X",
                        stringResource(R.string.legend_scan),
                        enabled = idle && uiState.selectedFolderUri != null && keysReady,
                        emphasised = selected == 0 && uiState.selectedFolderUri != null,
                        onClick = onScan,
                    ),
                )
                add(
                    LegendAction(
                        "Y",
                        when {
                            selected == 0 && tool.mode == ToolMode.Converter -> stringResource(R.string.legend_convert_idle)
                            selected == 0 -> stringResource(R.string.legend_apply_idle)
                            tool.mode == ToolMode.Converter -> pluralStringResource(R.plurals.legend_convert, selected, selected)
                            else -> pluralStringResource(R.plurals.legend_apply, selected, selected)
                        },
                        enabled = idle && selected > 0,
                        emphasised = selected > 0,
                        onClick = onApply,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HeaderStrip(
    tool: ToolEntry?,
    uiState: MimirUiState,
    contentFocus: FocusRequester,
    onHome: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    val palette = LocalMimirPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .heightIn(min = 52.dp)
            .focusProperties { down = contentFocus },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The breadcrumb takes the remaining width and truncates, so the storage readouts never get pushed off.
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (tool != null) {
                IconButton(onClick = onHome, modifier = Modifier.focusRing(CircleShape)) {
                    Icon(Icons.Outlined.Home, contentDescription = stringResource(R.string.go_home), tint = palette.text)
                }
            }
            MimirMark(Modifier.size(34.dp), contentDescription = stringResource(R.string.app_logo))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = palette.text, maxLines = 1)
            if (tool != null) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = palette.textMuted)
                Text(
                    stringResource(tool.toolName),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        uiState.volumes.forEach { volume -> VolumeReadout(volume) }
        IconButton(onClick = onToggleTheme, modifier = Modifier.focusRing(CircleShape)) {
            Icon(
                if (uiState.useDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = stringResource(R.string.toggle_dark_mode),
                tint = palette.text,
            )
        }
    }
}

/** One volume's free space: its icon, used space as the memory card's block meter, and what is left. */
@Composable
private fun VolumeReadout(volume: RomTreeRepository.VolumeSpace) {
    val palette = LocalMimirPalette.current
    val storage = volume.info
    val (icon, name) = when (volume.kind) {
        RomTreeRepository.VolumeKind.Internal -> Icons.Outlined.PhoneAndroid to stringResource(R.string.storage_internal)
        RomTreeRepository.VolumeKind.SdCard -> Icons.Outlined.SdCard to stringResource(R.string.storage_sd_card)
    }
    val free = stringResource(R.string.storage_free_header, formatBytes(storage.freeBytes))
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$name, $free" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(18.dp))
        BlockProgressBar(
            progress = 1f - storage.freeBytes.toFloat() / storage.totalBytes.coerceAtLeast(1L),
            modifier = Modifier.width(56.dp),
            blocks = 8,
            height = 8.dp,
            ink = palette.textMuted,
        )
        Text(
            free,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = palette.textMuted,
            maxLines = 1,
        )
    }
}

/**
 * A tool page as the memory card's data view: setup on the left (what to read and how), the data
 * itself on the right (what Mimir found and will write). Narrow screens stack the two.
 */
@Composable
private fun ToolScreen(
    tool: ToolEntry,
    uiState: MimirUiState,
    contentFocus: FocusRequester,
    queueSortOption: QueueSortOption,
    onQueueSortOptionChanged: (QueueSortOption) -> Unit,
    onSelectFolder: () -> Unit,
    onSelectVitaOutput: () -> Unit,
    onSelectNszKeys: () -> Unit,
    onSelectEsDeFolder: () -> Unit,
    onPickEsDeSystemFolder: (String) -> Unit,
    onPresetSelected: (FrontendPreset) -> Unit,
    onChdSystemSelected: (ChdSystem) -> Unit,
    onChdDiscTypeSelected: (ChdDiscType) -> Unit,
    onScanHiddenFoldersChanged: (Boolean) -> Unit,
    onVitaQueryChanged: (String) -> Unit,
    onVitaShortcutFormatSelected: (VitaShortcutFormat) -> Unit,
    onVitaShortcutAdd: (VitaApp) -> Unit,
    onVitaShortcutRemove: (VitaApp) -> Unit,
    onChangeSelection: (String, Boolean) -> Unit,
    onSelectAllChanges: () -> Unit,
    onDeselectAllChanges: () -> Unit,
    onStopNow: () -> Unit,
    onStopAfterCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val idle = !uiState.isBusy
    val setup: @Composable ColumnScope.() -> Unit = {
        SetupPanel {
            when (tool.mode) {
                ToolMode.VitaAppIds -> {
                    FolderRow(
                        label = stringResource(R.string.vita_output_directory),
                        folderName = uiState.vitaOutputName,
                        onSelect = onSelectVitaOutput,
                        buttonModifier = Modifier.focusRequester(contentFocus),
                    )
                    VitaFormatOptions(uiState.vitaShortcutFormat, enabled = idle, onSelected = onVitaShortcutFormatSelected)
                }
                ToolMode.EsDeSystems -> {
                    FolderRow(
                        label = stringResource(R.string.esde_selected_folder),
                        folderName = uiState.esDeRootName,
                        onSelect = onSelectEsDeFolder,
                        enabled = idle,
                        buttonModifier = Modifier.focusRequester(contentFocus),
                    )
                    PanelRule()
                    FolderRow(
                        label = stringResource(R.string.esde_rom_root),
                        folderName = uiState.selectedFolderName,
                        onSelect = onSelectFolder,
                        enabled = idle,
                        primary = false,
                    )
                }
                else -> {
                    FolderRow(
                        label = stringResource(R.string.control_selected_rom_folder),
                        folderName = uiState.selectedFolderName,
                        onSelect = onSelectFolder,
                        enabled = idle,
                        buttonModifier = Modifier.focusRequester(contentFocus),
                    )
                    when (tool.section) {
                        AppSection.Organizer -> OrganizerOptions(
                            selectedPreset = uiState.selectedPreset,
                            onPresetSelected = onPresetSelected,
                            scanHiddenFolders = uiState.scanHiddenFolders,
                            onScanHiddenFoldersChanged = onScanHiddenFoldersChanged,
                            enabled = idle,
                        )
                        AppSection.ChdMan -> ChdOptions(
                            system = uiState.selectedChdSystem,
                            discType = uiState.selectedChdDiscType,
                            enabled = idle,
                            onSystemSelected = onChdSystemSelected,
                            onDiscTypeSelected = onChdDiscTypeSelected,
                        )
                        AppSection.Nsz -> NszKeysRow(configured = uiState.nszKeysConfigured, enabled = idle, onSelect = onSelectNszKeys)
                        else -> Unit
                    }
                }
            }
        }
    }
    val data: @Composable ColumnScope.() -> Unit = {
        DataPaneProgress(uiState, onStopNow, onStopAfterCurrent)
        uiState.message?.let { StatusLine(it) }
        when (tool.mode) {
            ToolMode.VitaAppIds -> VitaDatabasePane(
                vitaQuery = uiState.vitaQuery,
                databaseSize = uiState.vitaDatabaseSize,
                searchResults = uiState.vitaSearchResults,
                addedShortcuts = uiState.addedVitaShortcuts,
                isBusy = uiState.isBusy,
                onVitaQueryChanged = onVitaQueryChanged,
                onAdd = onVitaShortcutAdd,
                onRemove = onVitaShortcutRemove,
            )
            ToolMode.EsDeSystems -> EsDeSystemsPane(uiState.esDeSystems, isBusy = uiState.isBusy, onPickFolder = onPickEsDeSystemFolder)
            else -> ScanResults(
                tool = tool,
                uiState = uiState,
                queueSortOption = queueSortOption,
                onQueueSortOptionChanged = onQueueSortOptionChanged,
                onChangeSelection = onChangeSelection,
                onSelectAllChanges = onSelectAllChanges,
                onDeselectAllChanges = onDeselectAllChanges,
            )
        }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= 700.dp) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                        .focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = setup,
                )
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                        .focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = data,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                setup()
                data()
            }
        }
    }
}

/** A running scan, apply or conversion, shown in place at the top of the data pane. */
@Composable
private fun DataPaneProgress(uiState: MimirUiState, onStopNow: () -> Unit, onStopAfterCurrent: () -> Unit) {
    val report = uiState.conversionReport
    if (uiState.selectedMode == ToolMode.Converter && uiState.isBusy && report != null) {
        ConversionProgressPanel(
            report = report,
            currentJobLabel = uiState.currentJobLabel,
            currentJobProgress = uiState.currentJobProgress ?: 0f,
            canStopAfterCurrent = uiState.operationProgressLabel != null && uiState.stopRequest == StopRequest.None,
            stopNowEnabled = uiState.stopRequest != StopRequest.Now,
            onStopNow = onStopNow,
            onStopAfterCurrent = onStopAfterCurrent,
        )
        return
    }
    val label = uiState.operationProgressLabel ?: uiState.scanProgressLabel
    if (label != null) {
        ProgressPanel(label, progress = if (uiState.operationProgressLabel != null) uiState.operationProgress else null)
    } else if (uiState.isBusy) {
        BlockProgressBar(progress = null)
    }
}

@Composable
private fun ScanResults(
    tool: ToolEntry,
    uiState: MimirUiState,
    queueSortOption: QueueSortOption,
    onQueueSortOptionChanged: (QueueSortOption) -> Unit,
    onChangeSelection: (String, Boolean) -> Unit,
    onSelectAllChanges: () -> Unit,
    onDeselectAllChanges: () -> Unit,
) {
    val palette = LocalMimirPalette.current
    val plan = uiState.previewPlan
    val enabled = !uiState.isBusy
    if (uiState.selectedMode == ToolMode.Converter && !uiState.isBusy) {
        uiState.conversionReport?.let { ConversionReportPanel(it) }
    }
    if (plan == null || plan.changes.isEmpty()) {
        if (uiState.isBusy) return
        val art: @Composable () -> Unit = {
            SaveBlockArt(tool.art, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), hole = palette.fieldTop, modifier = Modifier.size(96.dp))
        }
        when {
            uiState.selectedFolderUri == null -> EmptyPane(
                title = stringResource(R.string.data_no_folder_title),
                body = stringResource(R.string.data_no_folder_body),
                art = art,
            )
            plan == null -> EmptyPane(
                title = stringResource(R.string.data_not_scanned_title),
                body = stringResource(R.string.data_not_scanned_body, uiState.selectedFolderName),
                art = art,
            )
            else -> EmptyPane(
                title = stringResource(R.string.data_nothing_found_title),
                body = stringResource(R.string.data_nothing_found_body),
                art = art,
            )
        }
        return
    }
    PlanHeader(
        plan = plan,
        previewCount = uiState.previewCount,
        selectedPaths = uiState.selectedChangePaths,
        storageInfo = uiState.storageInfo,
        sortOption = queueSortOption,
        enabled = enabled,
        onSortOptionChanged = onQueueSortOptionChanged,
        onSelectAll = onSelectAllChanges,
        onDeselectAll = onDeselectAllChanges,
    )
    if (plan.mode == ToolMode.Converter) {
        ConversionQueueList(
            plan = plan,
            selectedPaths = uiState.selectedChangePaths,
            sortOption = queueSortOption,
            enabled = enabled,
            onChangeSelection = onChangeSelection,
        )
    } else {
        val largest = plan.changes.maxOfOrNull { it.sourceSizeBytes }?.coerceAtLeast(1L) ?: 1L
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            plan.changes.forEach { change ->
                ChangeRow(
                    plan = plan,
                    change = change,
                    selected = change.detailPath in uiState.selectedChangePaths,
                    enabled = enabled,
                    largest = largest,
                    onSelectedChange = { selected -> onChangeSelection(change.detailPath, selected) },
                )
            }
        }
    }
}

/** The footer link to About Mimir, at the left of the legend bar on home. */
@Composable
private fun AboutLink(onClick: () -> Unit) {
    val palette = LocalMimirPalette.current
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .focusRing()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.about_mimir), style = MaterialTheme.typography.labelLarge, color = palette.text)
    }
}
