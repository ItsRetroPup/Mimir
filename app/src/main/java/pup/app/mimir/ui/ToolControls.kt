package pup.app.mimir.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pup.app.mimir.R
import pup.app.mimir.domain.ChdDiscType
import pup.app.mimir.domain.ChdSystem
import pup.app.mimir.domain.ConverterTool
import pup.app.mimir.domain.FrontendPreset
import pup.app.mimir.domain.VitaShortcutFormat

@Composable
internal fun ControlCard(
    folderName: String,
    onSelectFolder: () -> Unit,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Label(stringResource(R.string.control_title))
            Text(stringResource(R.string.control_selected_rom_folder), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(folderName, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(
                onClick = onSelectFolder,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_select_rom_folder))
            }
        }
    }
}

@Composable
internal fun EsDeSystemsCard(
    rootFolderName: String,
    hasRootFolder: Boolean,
    romRootFolderName: String,
    systems: List<pup.app.mimir.data.EsDeSystem>,
    isBusy: Boolean,
    onSelectRomRoot: () -> Unit,
    onSelectRoot: () -> Unit,
    onRefresh: () -> Unit,
    onApply: () -> Unit,
    onPickFolder: (String) -> Unit,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label(stringResource(R.string.esde_title))
            Text(stringResource(R.string.esde_selected_folder), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(rootFolderName, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(onClick = onSelectRoot, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_select_esde_folder))
            }
            Text(stringResource(R.string.esde_rom_root), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(romRootFolderName, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            OutlinedButton(onClick = onSelectRomRoot, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_select_rom_root))
            }
            OutlinedButton(
                onClick = onRefresh,
                enabled = !isBusy && hasRootFolder,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_download_xmls)) }
            if (systems.isEmpty()) {
                Text(
                    stringResource(R.string.esde_download_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                )
            } else {
                Text(stringResource(R.string.esde_rom_folders), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                systems.forEach { system ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(system.fullName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (system.isDefault) stringResource(R.string.esde_default_folder) else system.romFolder,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                        OutlinedButton(
                            onClick = { onPickFolder(system.name) },
                            enabled = !isBusy,
                        ) { Text(stringResource(R.string.action_choose_folder)) }
                    }
                }
                Button(onClick = onApply, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_install_custom_systems))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun VitaControlCard(
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
            Label(stringResource(R.string.vita_title))
            Text(
                stringResource(R.string.vita_output_directory),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(outputFolderName, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(
                onClick = onSelectVitaOutput,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_select_output_folder))
            }
            Text(
                stringResource(R.string.vita_file_type),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            ChoiceRow(
                options = VitaShortcutFormat.entries,
                selected = shortcutFormat,
                enabled = !isBusy,
                label = { it.displayName },
                onSelect = onVitaShortcutFormatSelected,
            )
            Text(
                stringResource(R.string.vita_cocoon_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                stringResource(R.string.vita_database),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(pluralStringResource(R.plurals.vita_database_size, databaseSize, databaseSize), style = MaterialTheme.typography.bodyLarge)
            SearchField(
                query = vitaQuery,
                onQueryChanged = onVitaQueryChanged,
                label = stringResource(R.string.vita_search_label),
            )
            if (vitaQuery.isNotBlank()) {
                Text(
                    stringResource(R.string.vita_search_results),
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
                                    Text(app.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                                            contentDescription = stringResource(R.string.vita_delete_shortcut, app.title),
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onVitaShortcutAdd(app) },
                                        enabled = !isBusy,
                                    ) {
                                        Text(stringResource(R.string.action_quick_add))
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
internal fun OrganizerPresetCard(
    selectedPreset: FrontendPreset,
    onPresetSelected: (FrontendPreset) -> Unit,
    scanHiddenFolders: Boolean,
    onScanHiddenFoldersChanged: (Boolean) -> Unit,
    enabled: Boolean,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label(stringResource(R.string.organizer_frontend_target))
            ChoiceRow(
                options = FrontendPreset.entries,
                selected = selectedPreset,
                enabled = enabled,
                label = { if (it == FrontendPreset.Other) stringResource(R.string.preset_other) else it.displayName },
                onSelect = onPresetSelected,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.organizer_scan_hidden), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.organizer_scan_hidden_detail),
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
internal fun NszKeysCard(
    configured: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.nsz_keys_title), style = MaterialTheme.typography.titleMedium)
            Text(
                if (configured) {
                    stringResource(R.string.nsz_keys_configured)
                } else {
                    stringResource(R.string.nsz_keys_missing)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )
            Button(onClick = onSelect, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (configured) R.string.action_replace_keys else R.string.action_import_keys))
            }
        }
    }
}

@Composable
internal fun ConverterToolsOptionsCard(
    selectedTool: ConverterTool,
    system: ChdSystem,
    discType: ChdDiscType,
    enabled: Boolean,
    onSystemSelected: (ChdSystem) -> Unit,
    onDiscTypeSelected: (ChdDiscType) -> Unit,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (selectedTool == ConverterTool.Chd) {
                Text(
                    stringResource(R.string.chd_system),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                ChoiceRow(
                    options = ChdSystem.entries,
                    selected = system,
                    enabled = enabled,
                    label = { it.displayName },
                    onSelect = onSystemSelected,
                )
                if (system == ChdSystem.PlayStation2) {
                    Text(
                        stringResource(R.string.chd_conversion_type),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ChoiceRow(
                        options = ChdDiscType.entries,
                        selected = discType,
                        enabled = enabled,
                        label = { it.displayName },
                        onSelect = onDiscTypeSelected,
                    )
                    InfoPanel(
                        title = stringResource(R.string.chd_compatibility_title),
                        body = stringResource(R.string.chd_compatibility_body),
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
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
