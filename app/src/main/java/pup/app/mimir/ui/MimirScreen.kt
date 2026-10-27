package pup.app.mimir.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import pup.app.mimir.R
import pup.app.mimir.domain.ChdDiscType
import pup.app.mimir.domain.ChdSystem
import pup.app.mimir.domain.ConverterTool
import pup.app.mimir.domain.FrontendPreset
import pup.app.mimir.domain.ToolMode
import pup.app.mimir.domain.VitaShortcutFormat

@Composable
internal fun MimirScreen(
    uiState: pup.app.mimir.ui.MimirUiState,
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
    onVitaShortcutAdd: (pup.app.mimir.domain.VitaApp) -> Unit,
    onVitaShortcutRemove: (pup.app.mimir.domain.VitaApp) -> Unit,
    onChangeSelection: (String, Boolean) -> Unit,
    onSelectAllChanges: () -> Unit,
    onDeselectAllChanges: () -> Unit,
    onStart: () -> Unit,
    onApply: () -> Unit,
    onStopNow: () -> Unit,
    onStopAfterCurrent: () -> Unit,
) {
    val context = LocalContext.current
    val backgroundBrush = remember(uiState.useDarkMode) {
        if (uiState.useDarkMode) {
            Brush.verticalGradient(listOf(Color(0xFF100B14), Color(0xFF17101E), Color(0xFF0D0911)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFFAF7FC), Color(0xFFF1EAF5), Color(0xFFFDFBFE)))
        }
    }
    var currentSection by rememberSaveable { mutableStateOf(AppSection.Home) }
    var showApplyConfirmation by rememberSaveable { mutableStateOf(false) }
    var showEsDeConfirmation by rememberSaveable { mutableStateOf(false) }
    var pendingVitaAdd by remember { mutableStateOf<pup.app.mimir.domain.VitaApp?>(null) }
    var pendingVitaRemove by remember { mutableStateOf<pup.app.mimir.domain.VitaApp?>(null) }
    var queueSortOption by rememberSaveable { mutableStateOf(QueueSortOption.NameAscending.name) }
    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
    val selectTool: (ToolEntry) -> Unit = { tool ->
        onModeSelected(tool.mode)
        tool.converter?.let(onConverterToolSelected)
        currentSection = tool.section
    }
    BackHandler(enabled = currentSection != AppSection.Home) {
        currentSection = AppSection.Home
    }
    // Each screen starts at the top. With a controller or keyboard, focus starts on the screen's
    // first control, or on the tool card you came back from; touch input never shows focus.
    val scrollState = rememberSaveable(currentSection, saver = ScrollState.Saver) { ScrollState(0) }
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
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                tonalElevation = 0.dp,
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .focusProperties { down = contentFocus },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (currentSection != AppSection.Home) {
                            IconButton(
                                onClick = { currentSection = AppSection.Home },
                                modifier = Modifier.focusRing(CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Home,
                                    contentDescription = stringResource(R.string.go_home),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Image(
                            painter = painterResource(id = R.drawable.mimir_launcher_m),
                            contentDescription = stringResource(R.string.app_logo),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onDarkModeToggled(!uiState.useDarkMode) },
                            modifier = Modifier.focusRing(CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DarkMode,
                                contentDescription = stringResource(R.string.toggle_dark_mode),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
        if (currentSection != AppSection.Home &&
            uiState.selectedMode != ToolMode.VitaAppIds &&
            uiState.selectedMode != ToolMode.EsDeSystems &&
            !uiState.isBusy
        ) {
            val hasSelection = uiState.selectedChangePaths.isNotEmpty()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
            ) {
                Button(
                    onClick = if (hasSelection) {
                        { showApplyConfirmation = true }
                    } else {
                        onStart
                    },
                    enabled = if (hasSelection) true else uiState.selectedFolderUri != null,
                    modifier = Modifier.padding(8.dp).fillMaxWidth().focusRing(),
                ) {
                    Text(
                        when {
                            uiState.selectedMode == ToolMode.Converter && hasSelection ->
                                stringResource(R.string.action_convert_selected, uiState.selectedChangePaths.size)
                            hasSelection -> stringResource(R.string.action_apply_selected, uiState.selectedChangePaths.size)
                            else -> stringResource(R.string.action_scan_folder)
                        }
                    )
                }
            }
        }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            val isExpanded = maxWidth >= 840.dp && maxHeight >= 480.dp
            val progressLabel = uiState.operationProgressLabel ?: uiState.scanProgressLabel
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                    .verticalScroll(scrollState)
                    .padding(start = if (isExpanded) 96.dp else 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp)
                    .focusRequester(contentFocus)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentSection == AppSection.Home) {
                    HomeIntroSection()
                    ToolModeCards(
                        selectedMode = uiState.selectedMode,
                        selectedConverterTool = uiState.selectedConverterTool,
                        returnSection = lastToolSection,
                        returnFocus = returnFocus,
                        onPageSelected = selectTool,
                    )
                    SupportSection(
                        onOpenYoutube = { openUrl(YOUTUBE_CHANNEL_URL) },
                        onOpenKofi = { openUrl(KOFI_URL) },
                        onOpenBuyMeACoffee = { openUrl(BUY_ME_A_COFFEE_URL) },
                    )
                    HomeFooter()
                } else {
                    toolFor(currentSection)?.let { tool ->
                        ToolPageHeader(tool)
                    }
                    uiState.message?.let { message ->
                        InfoPanel(
                            title = stringResource(R.string.status),
                            body = message.text,
                            accent = if (message.isError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                        )
                    }
                    if (uiState.selectedMode == ToolMode.VitaAppIds) {
                        VitaControlCard(
                            outputFolderName = uiState.vitaOutputName,
                            onSelectVitaOutput = onSelectVitaOutput,
                            shortcutFormat = uiState.vitaShortcutFormat,
                            vitaQuery = uiState.vitaQuery,
                            databaseSize = uiState.vitaDatabaseSize,
                            searchResults = uiState.vitaSearchResults,
                            addedShortcuts = uiState.addedVitaShortcuts,
                            isBusy = uiState.isBusy,
                            onVitaQueryChanged = onVitaQueryChanged,
                            onVitaShortcutFormatSelected = onVitaShortcutFormatSelected,
                            onVitaShortcutAdd = { pendingVitaAdd = it },
                            onVitaShortcutRemove = { pendingVitaRemove = it },
                        )
                    } else if (uiState.selectedMode == ToolMode.EsDeSystems) {
                        EsDeSystemsCard(
                            rootFolderName = uiState.esDeRootName,
                            hasRootFolder = uiState.esDeRootUri != null,
                            romRootFolderName = uiState.selectedFolderName,
                            systems = uiState.esDeSystems,
                            isBusy = uiState.isBusy,
                            onSelectRomRoot = onSelectFolder,
                            onSelectRoot = onSelectEsDeFolder,
                            onRefresh = onRefreshEsDeSystems,
                            onApply = { showEsDeConfirmation = true },
                            onPickFolder = onPickEsDeSystemFolder,
                        )
                    } else {
                        ControlCard(
                            folderName = uiState.selectedFolderName,
                            onSelectFolder = onSelectFolder,
                        )
                    }

                    if (currentSection == AppSection.Organizer && uiState.selectedMode == ToolMode.MultiDiscOrganizer) {
                        OrganizerPresetCard(
                            selectedPreset = uiState.selectedPreset,
                            onPresetSelected = onPresetSelected,
                            scanHiddenFolders = uiState.scanHiddenFolders,
                            onScanHiddenFoldersChanged = onScanHiddenFoldersChanged,
                            enabled = !uiState.isBusy,
                        )
                    }

                    if (currentSection == AppSection.ChdMan && uiState.selectedMode == ToolMode.Converter) {
                        ConverterToolsOptionsCard(
                            selectedTool = uiState.selectedConverterTool,
                            system = uiState.selectedChdSystem,
                            discType = uiState.selectedChdDiscType,
                            enabled = !uiState.isBusy,
                            onSystemSelected = onChdSystemSelected,
                            onDiscTypeSelected = onChdDiscTypeSelected,
                        )
                    }

                    if (currentSection == AppSection.Nsz && uiState.selectedMode == ToolMode.Converter) {
                        NszKeysCard(
                            configured = uiState.nszKeysConfigured,
                            enabled = !uiState.isBusy,
                            onSelect = onSelectNszKeys,
                        )
                    }

                    if (uiState.isBusy && progressLabel == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (uiState.selectedMode != ToolMode.VitaAppIds) uiState.previewPlan?.let { plan ->
                        if (uiState.selectedMode == ToolMode.Converter) {
                            ConversionQueueTable(
                                plan = plan,
                                selectedPaths = uiState.selectedChangePaths,
                                sortOption = QueueSortOption.valueOf(queueSortOption),
                                enabled = !uiState.isBusy,
                                storageInfo = uiState.storageInfo,
                                onSortOptionChanged = { queueSortOption = it.name },
                                onSelectAll = onSelectAllChanges,
                                onDeselectAll = onDeselectAllChanges,
                                onChangeSelection = onChangeSelection,
                            )
                        }
                        PreviewSummary(
                            previewCount = uiState.previewCount,
                            selectedCount = uiState.selectedChangePaths.size,
                            selectedPaths = uiState.selectedChangePaths,
                            plan = plan,
                            onApply = { showApplyConfirmation = true },
                            isBusy = uiState.isBusy,
                        )

                        if (uiState.selectedMode != ToolMode.Converter) {
                            plan.changes.forEach { change ->
                                ChangeCard(
                                    plan = plan,
                                    change = change,
                                    selected = change.detailPath in uiState.selectedChangePaths,
                                    enabled = !uiState.isBusy,
                                    onSelectedChange = { selected ->
                                        onChangeSelection(change.detailPath, selected)
                                    },
                                )
                            }
                        }
                    }

                    if (uiState.selectedMode == ToolMode.Converter) uiState.conversionReport?.let { report ->
                        ConversionReportCard(report)
                    }

                }
        }
            if (isExpanded) {
                NavigationRail(
                    modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ) {
                    NavigationRailItem(
                        selected = currentSection == AppSection.Home,
                        onClick = { currentSection = AppSection.Home },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = stringResource(R.string.home)) },
                        label = { Text(stringResource(R.string.home)) },
                    )
                    (utilities + spaceSavers).forEach { tool ->
                        NavigationRailItem(
                            selected = currentSection == tool.section,
                            onClick = { selectTool(tool) },
                            icon = { Icon(tool.icon, contentDescription = stringResource(tool.goal)) },
                            label = { Text(stringResource(tool.toolName)) },
                        )
                    }
                }
            }
            val progressModifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                )
            if (uiState.selectedMode == ToolMode.Converter && uiState.isBusy && uiState.conversionReport != null) {
                ConversionProgressToast(
                    report = uiState.conversionReport,
                    currentJobLabel = uiState.currentJobLabel,
                    currentJobProgress = uiState.currentJobProgress ?: 0f,
                    canStopAfterCurrent = uiState.operationProgressLabel != null &&
                        uiState.stopRequest == pup.app.mimir.data.StopRequest.None,
                    stopNowEnabled = uiState.stopRequest != pup.app.mimir.data.StopRequest.Now,
                    onStopNow = onStopNow,
                    onStopAfterCurrent = onStopAfterCurrent,
                    modifier = progressModifier,
                )
            } else {
                progressLabel?.let { label ->
                    ProgressToast(label = label, progress = uiState.operationProgress, modifier = progressModifier)
                }
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
