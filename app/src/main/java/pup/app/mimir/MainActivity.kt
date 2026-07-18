package pup.app.mimir

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pup.app.mimir.domain.FileOperation
import pup.app.mimir.domain.FrontendPreset
import pup.app.mimir.domain.OperationPlan
import pup.app.mimir.domain.ToolMode
import pup.app.mimir.domain.VitaShortcutFormat
import pup.app.mimir.ui.MimirViewModel
import pup.app.mimir.R

private enum class AppSection {
    Home,
    Zipper,
    Organizer,
    Vita,
    About,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MimirViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val folderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                viewModel.onFolderSelected(uri)
            }
            val vitaOutputLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                viewModel.onVitaOutputSelected(uri)
            }

            MimirTheme(useDarkMode = uiState.useDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MimirScreen(
                        uiState = uiState,
                        onSelectFolder = { folderLauncher.launch(null) },
                        onSelectVitaOutput = { vitaOutputLauncher.launch(null) },
                        onModeSelected = viewModel::updateMode,
                        onPresetSelected = viewModel::updatePreset,
                        onScanHiddenFoldersChanged = viewModel::updateScanHiddenFolders,
                        onDarkModeToggled = viewModel::updateDarkMode,
                        onVitaQueryChanged = viewModel::updateVitaQuery,
                        onVitaShortcutFormatSelected = viewModel::updateVitaShortcutFormat,
                        onVitaShortcutAdd = viewModel::addVitaShortcut,
                        onVitaShortcutRemove = viewModel::removeVitaShortcut,
                        onChangeSelection = viewModel::updateChangeSelection,
                        onStart = viewModel::scan,
                        onApply = viewModel::applyChanges,
                    )
                }
            }
        }
    }
}

@Composable
private fun MimirTheme(
    useDarkMode: Boolean,
    content: @Composable () -> Unit,
) {
    val lightScheme = lightColorScheme(
        primary = Color(0xFF007B83),
        onPrimary = Color.White,
        secondary = Color(0xFF00C8DE),
        onSecondary = Color(0xFF002D33),
        tertiary = Color(0xFF59646A),
        background = Color(0xFFF0F4F8),
        onBackground = Color(0xFF111316),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF161A1D),
        surfaceVariant = Color(0xFFE3E9EE),
        outline = Color(0xFF97A3AA),
    )
    val darkScheme = darkColorScheme(
        primary = Color(0xFF7AD5DD),
        onPrimary = Color(0xFF00363A),
        secondary = Color(0xFF00E5FF),
        onSecondary = Color(0xFF00363D),
        tertiary = Color(0xFFC3C7CB),
        background = Color(0xFF111316),
        onBackground = Color(0xFFE2E2E6),
        surface = Color(0xFF1A1C1F),
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = Color(0xFF282A2D),
        outline = Color(0xFF3E494A),
    )

    val typography = MaterialTheme.typography.copy(
        displaySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.0.sp,
        ),
    )

    MaterialTheme(
        colorScheme = if (useDarkMode) darkScheme else lightScheme,
        typography = typography,
        content = content,
    )
}

@Composable
private fun MimirScreen(
    uiState: pup.app.mimir.ui.MimirUiState,
    onSelectFolder: () -> Unit,
    onSelectVitaOutput: () -> Unit,
    onModeSelected: (ToolMode) -> Unit,
    onPresetSelected: (FrontendPreset) -> Unit,
    onScanHiddenFoldersChanged: (Boolean) -> Unit,
    onDarkModeToggled: (Boolean) -> Unit,
    onVitaQueryChanged: (String) -> Unit,
    onVitaShortcutFormatSelected: (VitaShortcutFormat) -> Unit,
    onVitaShortcutAdd: (pup.app.mimir.domain.VitaApp) -> Unit,
    onVitaShortcutRemove: (pup.app.mimir.domain.VitaApp) -> Unit,
    onChangeSelection: (String, Boolean) -> Unit,
    onStart: () -> Unit,
    onApply: () -> Unit,
) {
    val context = LocalContext.current
    val backgroundBrush = remember(uiState.useDarkMode) {
        if (uiState.useDarkMode) {
            Brush.verticalGradient(listOf(Color(0xFF111316), Color(0xFF15181C), Color(0xFF0F1114)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFF0F4F8), Color(0xFFE7EEF3), Color(0xFFF7FAFC)))
        }
    }
    var currentSection by rememberSaveable { mutableStateOf(AppSection.Home) }
    var showApplyConfirmation by rememberSaveable { mutableStateOf(false) }
    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (currentSection != AppSection.Home) {
                            IconButton(onClick = { currentSection = AppSection.Home }) {
                                Icon(
                                    imageVector = Icons.Outlined.Home,
                                    contentDescription = "Go home",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Image(
                            painter = painterResource(id = R.drawable.mimir_launcher_m),
                            contentDescription = "Mimir logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = "Mimir",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onDarkModeToggled(!uiState.useDarkMode) }) {
                            Icon(
                                imageVector = Icons.Outlined.DarkMode,
                                contentDescription = "Toggle dark mode",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { currentSection = AppSection.About }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "About",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            val progressLabel = uiState.operationProgressLabel ?: uiState.scanProgressLabel
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentSection == AppSection.About) {
                    item {
                        AboutSection(
                            onOpenYoutube = { openUrl(YOUTUBE_CHANNEL_URL) },
                            onOpenGithub = { openUrl(GITHUB_PAGE_URL) },
                        )
                    }
                } else {
                    item {
                        HeroSection(
                            currentSection = currentSection,
                        )
                    }

                    if (currentSection == AppSection.Home) {
                        item {
                            ToolModeCards(
                                selectedMode = uiState.selectedMode,
                                onModeSelected = {
                                    onModeSelected(it)
                                    currentSection = when (it) {
                                        ToolMode.RomZipper -> AppSection.Zipper
                                        ToolMode.MultiDiscOrganizer -> AppSection.Organizer
                                        ToolMode.VitaAppIds -> AppSection.Vita
                                    }
                                },
                            )
                        }
                    } else {
                        item {
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
                                    onVitaShortcutAdd = onVitaShortcutAdd,
                                    onVitaShortcutRemove = onVitaShortcutRemove,
                                )
                            } else {
                                ControlCard(
                                    folderName = uiState.selectedFolderName,
                                    onSelectFolder = onSelectFolder,
                                )
                            }
                        }

                        if (currentSection == AppSection.Organizer && uiState.selectedMode == ToolMode.MultiDiscOrganizer) {
                            item {
                                OrganizerPresetCard(
                                    selectedPreset = uiState.selectedPreset,
                                    onPresetSelected = onPresetSelected,
                                    scanHiddenFolders = uiState.scanHiddenFolders,
                                    onScanHiddenFoldersChanged = onScanHiddenFoldersChanged,
                                    enabled = !uiState.isBusy,
                                )
                            }
                        }

                        if (uiState.selectedMode != ToolMode.VitaAppIds) {
                            item {
                                ActionCard(
                                    onStart = onStart,
                                    onApply = { showApplyConfirmation = true },
                                    canStart = uiState.selectedFolderUri != null && !uiState.isBusy,
                                    canApply = uiState.selectedChangePaths.isNotEmpty() && !uiState.isBusy,
                                )
                            }
                        }

                        if (uiState.isBusy && progressLabel == null) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        uiState.message?.let { message ->
                            item {
                                InfoPanel(
                                    title = "Status",
                                    body = message,
                                    accent = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }

                        if (uiState.selectedMode != ToolMode.VitaAppIds) uiState.previewPlan?.let { plan ->
                            item {
                                PreviewSummary(
                                    previewCount = uiState.previewCount,
                                    selectedCount = uiState.selectedChangePaths.size,
                                    plan = plan,
                                    onApply = { showApplyConfirmation = true },
                                    isBusy = uiState.isBusy,
                                )
                            }

                            items(plan.changes) { change ->
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
                }
            }
            progressLabel?.let { label ->
                ProgressToast(
                    label = label,
                    progress = uiState.operationProgress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp,
                        ),
                )
            }
        }
    }

    if (showApplyConfirmation) {
        val changeCount = uiState.selectedChangePaths.size
        AlertDialog(
            onDismissRequest = { showApplyConfirmation = false },
            title = { Text("Apply changes?") },
            text = {
                Text(
                    "This will apply $changeCount planned changes to your selected ROM folder. " +
                        "Original files may be moved or removed as shown in the preview, and Mimir cannot undo this action."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApplyConfirmation = false
                        onApply()
                    },
                    enabled = !uiState.isBusy && changeCount > 0,
                ) {
                    Text("APPLY CHANGES")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showApplyConfirmation = false }) {
                    Text("CANCEL")
                }
            },
        )
    }
}

@Composable
private fun HeroSection(
    currentSection: AppSection,
) {
    val title = when (currentSection) {
        AppSection.Home -> "Welcome, Brother"
        AppSection.Organizer -> "Organizer"
        AppSection.Zipper -> "Zipper"
        AppSection.Vita -> "Vita Shortcuts"
        AppSection.About -> "About"
    }
    val body = when (currentSection) {
        AppSection.Home ->
            "Select your tool below"
        AppSection.Organizer ->
            "Organises your multi-disc ROMs into the appropriate format for your chosen frontend"
        AppSection.Zipper ->
            "Compresses compatible ROM files to .zip to save some space"
        AppSection.Vita ->
            "Search the built-in Vita shortcut database, queue titles, and generate scraper-friendly .psvita files on-device"
        AppSection.About ->
            "Mimir is a simple tool utilty designed by RetroPup. Check out RetroPup on YouTube for guides, tips and tricks for the AYN Thor and other retro handhelds!"
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun AboutSection(
    onOpenYoutube: () -> Unit,
    onOpenGithub: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoPanel(
            title = "About Mimir",
            body = "Mimir is a toolkit designed to streamline your ROMs library",
            accent = MaterialTheme.colorScheme.primary,
        )
        StyledCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Label("LINKS")
                AboutLinkCard(
                    title = "GitHub Page",
                    body = GITHUB_PAGE_URL,
                    buttonLabel = "OPEN GITHUB",
                    enabled = true,
                    icon = Icons.Outlined.GridView,
                    onClick = onOpenGithub,
                )
                AboutLinkCard(
                    title = "Follow RetroPup on YouTube!",
                    body = YOUTUBE_CHANNEL_URL,
                    buttonLabel = "OPEN CHANNEL",
                    enabled = true,
                    icon = Icons.Outlined.Subscriptions,
                    onClick = onOpenYoutube,
                )
            }
        }
    }
}

@Composable
private fun AboutLinkCard(
    title: String,
    body: String,
    buttonLabel: String,
    enabled: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun ControlCard(
    folderName: String,
    onSelectFolder: () -> Unit,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Label("SYSTEM CONTROL")
            Text("Selected ROM folder", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(folderName, style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onSelectFolder) {
                    Text("SELECT FOLDER")
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun VitaControlCard(
    outputFolderName: String,
    onSelectVitaOutput: () -> Unit,
    shortcutFormat: VitaShortcutFormat,
    vitaQuery: String,
    databaseSize: Int,
    searchResults: List<pup.app.mimir.domain.VitaApp>,
    addedShortcuts: Map<String, String>,
    isBusy: Boolean,
    onVitaQueryChanged: (String) -> Unit,
    onVitaShortcutFormatSelected: (VitaShortcutFormat) -> Unit,
    onVitaShortcutAdd: (pup.app.mimir.domain.VitaApp) -> Unit,
    onVitaShortcutRemove: (pup.app.mimir.domain.VitaApp) -> Unit,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Label("VITA SHORTCUTS")
            Text(
                "Shortcut output directory",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(outputFolderName, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onSelectVitaOutput) {
                Text("SELECT OUTPUT")
            }
            Text(
                "Shortcut file type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VitaShortcutFormat.entries.forEach { format ->
                    FilterChip(
                        selected = shortcutFormat == format,
                        onClick = { onVitaShortcutFormatSelected(format) },
                        label = { Text(format.displayName) },
                        enabled = !isBusy,
                    )
                }
            }
            Text(
                "For Cocoon users, select .dpt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                "Shortcut database",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("$databaseSize titles ready for search", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = vitaQuery,
                onValueChange = onVitaQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search title or app ID") },
                singleLine = true,
            )
            if (vitaQuery.isNotBlank()) {
                Text(
                    "Search results",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    searchResults.forEach { app ->
                        val added = addedShortcuts.containsKey(app.titleId)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(app.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        app.titleId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    )
                                }
                                if (added) {
                                    IconButton(
                                        onClick = { onVitaShortcutRemove(app) },
                                        enabled = !isBusy,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete shortcut",
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onVitaShortcutAdd(app) },
                                        enabled = !isBusy,
                                    ) {
                                        Text("QUICK ADD")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    onStart: () -> Unit,
    onApply: () -> Unit,
    canStart: Boolean,
    canApply: Boolean,
) {
    StyledCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onStart,
                enabled = canStart,
                modifier = Modifier.weight(1f),
            ) {
                Text("START")
            }
            OutlinedButton(
                onClick = onApply,
                enabled = canApply,
                modifier = Modifier.weight(1f),
            ) {
                Text("APPLY")
            }
        }
    }
}

@Composable
private fun ToolModeCards(
    selectedMode: ToolMode,
    onModeSelected: (ToolMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolCard(
            title = "Zipper",
            body = "Compress zip-safe handheld ROMs after reviewing and confirming the planned changes.",
            cta = "LAUNCH UTILITY",
            selected = selectedMode == ToolMode.RomZipper,
            icon = Icons.Outlined.FolderZip,
            onClick = { onModeSelected(ToolMode.RomZipper) },
        )
        ToolCard(
            title = "Organizer",
            body = "Clean up multi-disc libraries and emit frontend-safe folder and playlist structures.",
            cta = "BROWSE FILES",
            selected = selectedMode == ToolMode.MultiDiscOrganizer,
            icon = Icons.Outlined.Archive,
            onClick = { onModeSelected(ToolMode.MultiDiscOrganizer) },
        )
        ToolCard(
            title = "Vita Shortcuts",
            body = "Search a built-in PSVita shortcut database and prepare title-based .psvita files in your chosen output directory.",
            cta = "BUILD SHORTCUTS",
            selected = selectedMode == ToolMode.VitaAppIds,
            icon = Icons.Outlined.Home,
            onClick = { onModeSelected(ToolMode.VitaAppIds) },
        )
    }
}

@Composable
private fun ToolCard(
    title: String,
    body: String,
    cta: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                if (selected) {
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                } else {
                    listOf(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    )
                }
            )
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.primary,
                )
            }
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            Text(
                text = "$cta ->",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun OrganizerPresetCard(
    selectedPreset: FrontendPreset,
    onPresetSelected: (FrontendPreset) -> Unit,
    scanHiddenFolders: Boolean,
    onScanHiddenFoldersChanged: (Boolean) -> Unit,
    enabled: Boolean,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label("FRONTEND TARGET")
            ChoiceRow(
                options = FrontendPreset.entries,
                selected = selectedPreset,
                enabled = enabled,
                label = { it.displayName },
                onSelect = onPresetSelected,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Scan hidden folders", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Extract ROMs from .-prefixed folders into the system folder before organizing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                Switch(
                    checked = scanHiddenFolders,
                    onCheckedChange = if (enabled) onScanHiddenFoldersChanged else null,
                )
            }
        }
    }
}

@Composable
private fun ProgressToast(
    label: String,
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                label,
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (progress == null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ThinProgress(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                )
        )
    }
}

@Composable
private fun InfoPanel(
    title: String,
    body: String,
    accent: Color,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = accent)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f))
        }
    }
}

@Composable
private fun ChangeCard(
    plan: OperationPlan,
    change: pup.app.mimir.domain.PlannedChange,
    selected: Boolean,
    enabled: Boolean,
    onSelectedChange: (Boolean) -> Unit,
) {
    val chipText = when (plan.mode) {
        ToolMode.MultiDiscOrganizer -> "${change.sourceFiles.size} DISCS"
        ToolMode.RomZipper -> "ZIP"
        ToolMode.VitaAppIds -> "VITA"
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = when (plan.mode) {
                        ToolMode.RomZipper -> Icons.Outlined.FolderZip
                        ToolMode.VitaAppIds -> Icons.Outlined.Home
                        else -> Icons.Outlined.Folder
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = onSelectedChange,
                        enabled = enabled,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            chipText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            Text(change.title, style = MaterialTheme.typography.titleLarge)
            Text(
                "${change.detailLabel} • ${change.detailPath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sources", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                change.sourceFiles.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Targets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                change.targetFiles.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun StyledCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun <T> ChoiceRow(
    options: Iterable<T>,
    selected: T,
    enabled: Boolean,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                enabled = enabled,
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun PreviewSummary(
    previewCount: Int,
    selectedCount: Int,
    plan: OperationPlan,
    onApply: () -> Unit,
    isBusy: Boolean,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label("PREVIEW")
            Text(
                when (plan.mode) {
                    ToolMode.MultiDiscOrganizer -> "$previewCount multi-disc sets detected; $selectedCount selected."
                    ToolMode.RomZipper -> "$previewCount ROMs matched the zip whitelist; $selectedCount selected."
                    ToolMode.VitaAppIds -> "$previewCount Vita shortcuts ready; $selectedCount selected."
                },
                style = MaterialTheme.typography.headlineMedium,
            )
            ThinProgress(
                progress = (plan.operations.size.coerceAtMost(20) / 20f).coerceAtLeast(0.12f)
            )
            if (plan.conflicts.isNotEmpty()) {
                Text("Conflicts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                plan.conflicts.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            } else {
                Text("No conflicts detected.", style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onApply,
                enabled = !isBusy && selectedCount > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Apply")
            }
        }
    }
}

private fun FileOperation.describe(): String = when (this) {
    is FileOperation.CreateDirectory -> "Create folder $relativePath"
    is FileOperation.MoveFile -> "Move $sourcePath -> $targetPath"
    is FileOperation.WriteTextFile -> "Write playlist $relativePath"
    is FileOperation.ZipFile -> "Zip $sourcePath -> $targetPath"
}

private const val GITHUB_PAGE_URL = "https://github.com/ItsRetroPup/Mimir"
private const val YOUTUBE_CHANNEL_URL = "https://youtube.com/@ItsRetroPup"
