package pup.app.mimir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pup.app.mimir.R
import pup.app.mimir.data.EsDeSystem
import pup.app.mimir.domain.ChdDiscType
import pup.app.mimir.domain.ChdSystem
import pup.app.mimir.domain.FrontendPreset
import pup.app.mimir.domain.VitaApp
import pup.app.mimir.domain.VitaShortcutFormat

/**
 * The setup pane as one console panel: each setting is a row with its label on the left and its
 * control on the right, separated by rules, so a tool's whole setup fits the handheld screen.
 */
@Composable
internal fun SetupPanel(content: @Composable ColumnScope.() -> Unit) {
    ConsolePanel(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

/** Which folder (or, with a file [icon], which file) the tool reads or writes, and the button that changes it. */
@Composable
internal fun FolderRow(
    label: String,
    folderName: String,
    onSelect: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = true,
    buttonModifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Folder,
    buttonLabel: String = stringResource(R.string.action_choose_folder),
    detail: String? = null,
) {
    val palette = LocalMimirPalette.current
    // The folder gets the row's full width so a picked path is never cut short; the button sits under it.
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = palette.textMuted)
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(folderName, style = MaterialTheme.typography.titleSmall, color = palette.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
        if (primary) {
            InkButton(onClick = onSelect, enabled = enabled, modifier = buttonModifier) { Text(buttonLabel, maxLines = 1) }
        } else {
            OutlinedButton(onClick = onSelect, enabled = enabled, modifier = buttonModifier.focusRing()) { Text(buttonLabel, maxLines = 1) }
        }
    }
}

/** A setting's name on the left and its control on the right. */
@Composable
private fun OptionRow(label: String, detail: String? = null, control: @Composable () -> Unit) {
    val palette = LocalMimirPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = palette.text)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
        }
        control()
    }
}

@Composable
internal fun OrganizerOptions(
    selectedPreset: FrontendPreset,
    onPresetSelected: (FrontendPreset) -> Unit,
    scanHiddenFolders: Boolean,
    onScanHiddenFoldersChanged: (Boolean) -> Unit,
    enabled: Boolean,
) {
    PanelRule()
    OptionRow(stringResource(R.string.organizer_frontend_target)) {
        ChoiceRow(
            options = FrontendPreset.entries,
            selected = selectedPreset,
            enabled = enabled,
            label = { if (it == FrontendPreset.Other) stringResource(R.string.preset_other) else it.displayName },
            onSelect = onPresetSelected,
        )
    }
    PanelRule()
    OptionRow(
        label = stringResource(R.string.organizer_scan_hidden),
        detail = stringResource(R.string.organizer_scan_hidden_detail),
    ) {
        Switch(checked = scanHiddenFolders, onCheckedChange = if (enabled) onScanHiddenFoldersChanged else null)
    }
}

@Composable
internal fun ChdOptions(
    system: ChdSystem,
    discType: ChdDiscType,
    enabled: Boolean,
    onSystemSelected: (ChdSystem) -> Unit,
    onDiscTypeSelected: (ChdDiscType) -> Unit,
) {
    val palette = LocalMimirPalette.current
    PanelRule()
    // Seven systems do not fit beside a label, so this one setting stacks.
    Text(stringResource(R.string.chd_system), style = MaterialTheme.typography.titleSmall, color = palette.text)
    ChoiceRow(
        options = ChdSystem.entries,
        selected = system,
        enabled = enabled,
        label = { it.displayName },
        onSelect = onSystemSelected,
    )
    if (system == ChdSystem.PlayStation2) {
        PanelRule()
        OptionRow(stringResource(R.string.chd_conversion_type), detail = stringResource(R.string.chd_compatibility_body)) {
            ChoiceRow(
                options = ChdDiscType.entries,
                selected = discType,
                enabled = enabled,
                label = { it.displayName },
                onSelect = onDiscTypeSelected,
            )
        }
    }
}

@Composable
internal fun NszKeysRow(configured: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    PanelRule()
    OptionRow(
        label = stringResource(R.string.nsz_keys_title),
        detail = stringResource(if (configured) R.string.nsz_keys_configured else R.string.nsz_keys_missing),
    ) {
        val label = stringResource(if (configured) R.string.action_replace_keys else R.string.action_import_keys)
        if (configured) {
            OutlinedButton(onClick = onSelect, enabled = enabled, modifier = Modifier.focusRing()) { Text(label, maxLines = 1) }
        } else {
            InkButton(onClick = onSelect, enabled = enabled) {
                Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(label, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
internal fun VitaFormatOptions(
    shortcutFormat: VitaShortcutFormat,
    enabled: Boolean,
    onSelected: (VitaShortcutFormat) -> Unit,
) {
    PanelRule()
    OptionRow(stringResource(R.string.vita_file_type), detail = stringResource(R.string.vita_cocoon_hint)) {
        ChoiceRow(
            options = VitaShortcutFormat.entries,
            selected = shortcutFormat,
            enabled = enabled,
            label = { it.displayName },
            onSelect = onSelected,
        )
    }
}

/** The Vita data pane: the built-in title database, searched and added to the output folder. */
@Composable
internal fun VitaDatabasePane(
    vitaQuery: String,
    databaseSize: Int,
    searchResults: List<VitaApp>,
    addedShortcuts: Map<String, String>,
    isBusy: Boolean,
    onVitaQueryChanged: (String) -> Unit,
    onAdd: (VitaApp) -> Unit,
    onRemove: (VitaApp) -> Unit,
) {
    val palette = LocalMimirPalette.current
    Row(verticalAlignment = Alignment.Bottom) {
        Text(stringResource(R.string.vita_database), style = MaterialTheme.typography.headlineSmall, color = palette.text, modifier = Modifier.weight(1f))
        Text(
            pluralStringResource(R.plurals.vita_database_size, databaseSize, databaseSize),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = palette.textMuted,
        )
    }
    SearchField(query = vitaQuery, onQueryChanged = onVitaQueryChanged, label = stringResource(R.string.vita_search_label))
    if (vitaQuery.isNotBlank()) {
        searchResults.forEach { app ->
            val added = addedShortcuts.containsKey(app.titleId)
            DataRow(selected = added) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(app.title, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(app.titleId, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"), color = palette.textMuted)
                }
                if (added) {
                    IconButton(onClick = { onRemove(app) }, enabled = !isBusy, modifier = Modifier.focusRing(CircleShape)) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.vita_delete_shortcut, app.title), tint = palette.caution)
                    }
                } else {
                    OutlinedButton(onClick = { onAdd(app) }, enabled = !isBusy, modifier = Modifier.focusRing()) {
                        Text(stringResource(R.string.action_quick_add))
                    }
                }
            }
        }
    } else {
        EmptyPane(title = stringResource(R.string.vita_search_results), body = stringResource(R.string.vita_search_hint))
    }
}

/** The ES-DE data pane: every detected system with the ROM folder it will point at. */
@Composable
internal fun EsDeSystemsPane(
    systems: List<EsDeSystem>,
    isBusy: Boolean,
    onPickFolder: (String) -> Unit,
) {
    val palette = LocalMimirPalette.current
    if (systems.isEmpty()) {
        EmptyPane(title = stringResource(R.string.esde_rom_folders), body = stringResource(R.string.esde_download_hint))
        return
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(stringResource(R.string.esde_rom_folders), style = MaterialTheme.typography.headlineSmall, color = palette.text, modifier = Modifier.weight(1f))
        Text("${systems.size}", style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"), color = palette.textMuted)
    }
    systems.forEach { system ->
        DataRow {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(system.fullName, style = MaterialTheme.typography.titleMedium, color = palette.text)
                Text(
                    if (system.isDefault) stringResource(R.string.esde_default_folder) else system.romFolder,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(onClick = { onPickFolder(system.name) }, enabled = !isBusy, modifier = Modifier.focusRing()) {
                Text(stringResource(R.string.action_choose_folder))
            }
        }
    }
}

/** The ROM Patcher's setup: the ROM to patch and the patch to apply. */
@Composable
internal fun RomPatchOptions(
    state: RomPatchState,
    enabled: Boolean,
    onSelectRom: () -> Unit,
    onSelectPatch: () -> Unit,
    buttonModifier: Modifier = Modifier,
) {
    FolderRow(
        label = stringResource(R.string.patch_rom_label),
        folderName = state.rom?.name ?: stringResource(R.string.patch_no_rom),
        onSelect = onSelectRom,
        enabled = enabled,
        // The next step is the filled button: the ROM, then the patch, then Y to patch.
        primary = state.rom == null,
        buttonModifier = buttonModifier,
        icon = Icons.Outlined.InsertDriveFile,
        buttonLabel = stringResource(R.string.action_choose_rom),
    )
    PanelRule()
    FolderRow(
        label = stringResource(R.string.patch_file_label),
        folderName = state.patch?.name ?: stringResource(R.string.patch_no_patch),
        onSelect = onSelectPatch,
        enabled = enabled,
        primary = state.rom != null && state.patch == null,
        icon = Icons.Outlined.InsertDriveFile,
        buttonLabel = stringResource(R.string.action_choose_patch),
        detail = state.format?.let { stringResource(R.string.patch_format_detail, it.displayName) },
    )
}

/** The ROM Patcher's data pane: the patched copy Mimir will write, and where. */
@Composable
internal fun RomPatchPane(
    state: RomPatchState,
    isBusy: Boolean,
    onGrantFolder: () -> Unit,
    art: @Composable () -> Unit,
) {
    val palette = LocalMimirPalette.current
    val outputName = state.outputName
    if (outputName == null) {
        EmptyPane(title = stringResource(R.string.patch_empty_title), body = stringResource(R.string.patch_empty_body), art = art)
        return
    }
    Text(stringResource(R.string.patch_output_title), style = MaterialTheme.typography.headlineSmall, color = palette.text)
    DataRow(selected = state.outputFolder != null) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(outputName, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            state.outputFolder?.let { folder ->
                Text(stringResource(R.string.patch_output_folder, folder.label), style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
            }
            if (state.outputExists) {
                Text(stringResource(R.string.patch_output_exists), style = MaterialTheme.typography.bodySmall, color = palette.caution)
            }
        }
        state.format?.let { format ->
            Text(
                format.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
    if (state.outputFolder == null) {
        // Android lets Mimir write only inside folders the user has granted.
        ConsolePanel(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.patch_folder_access_title), style = MaterialTheme.typography.titleSmall, color = palette.text)
            Text(
                stringResource(R.string.patch_folder_access_body, state.rom?.name.orEmpty()),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
            )
            InkButton(onClick = onGrantFolder, enabled = !isBusy) { Text(stringResource(R.string.action_choose_folder), maxLines = 1) }
        }
    }
}

/** A row in a data pane, drawn like an entry in the memory card's file list. */
@Composable
internal fun DataRow(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalMimirPalette.current
    val ink = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) ink.copy(alpha = 0.1f) else palette.panel)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * A search box that a controller can move across without opening the keyboard: it stays read-only
 * until activated with A/Enter or a tap, and drops back to read-only when focus leaves.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    label: String,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) editing = true
        }
    }
    LaunchedEffect(editing) {
        if (editing) keyboard?.show()
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        readOnly = !editing,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (!it.isFocused) editing = false }
            .onPreviewKeyEvent { event ->
                when {
                    !editing && event.type == KeyEventType.KeyUp &&
                        event.key in setOf(Key.DirectionCenter, Key.ButtonA, Key.Enter, Key.NumPadEnter) -> {
                        editing = true
                        true
                    }
                    // A single-line field would otherwise keep Up/Down to move its cursor, trapping
                    // the controller once the keyboard has been closed.
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp ->
                        focusManager.moveFocus(FocusDirection.Up)
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown ->
                        focusManager.moveFocus(FocusDirection.Down)
                    else -> false
                }
            },
        label = { Text(label) },
        singleLine = true,
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}
