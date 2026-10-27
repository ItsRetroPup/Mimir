package pup.app.mimir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
            Label("SYSTEM CONTROL")
            Text("Selected ROM folder", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(folderName, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(
                onClick = onSelectFolder,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("SELECT ROM FOLDER")
            }
        }
    }
}

@Composable
internal fun EsDeSystemsCard(
    rootFolderName: String,
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
            Label("ES-DE CUSTOM SYSTEMS")
            Text("Selected ES-DE folder", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(rootFolderName, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(onClick = onSelectRoot, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("SELECT ES-DE FOLDER")
            }
            Text("ROM root folder", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(romRootFolderName, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            OutlinedButton(onClick = onSelectRomRoot, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("SELECT ROM ROOT")
            }
            OutlinedButton(
                onClick = onRefresh,
                enabled = !isBusy && rootFolderName != "No ES-DE folder selected",
                modifier = Modifier.fillMaxWidth(),
            ) { Text("DOWNLOAD LATEST XMLS") }
            if (systems.isEmpty()) {
                Text(
                    "Download the latest XMLs to configure the custom Android systems. They will be installed under custom_systems.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                )
            } else {
                Text("ROM folders", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                systems.forEach { system ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(system.fullName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (system.isDefault) "Default" else system.romFolder,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                        OutlinedButton(
                            onClick = { onPickFolder(system.name) },
                            enabled = !isBusy,
                        ) { Text("CHOOSE FOLDER") }
                    }
                }
                Button(onClick = onApply, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                    Text("INSTALL CUSTOM SYSTEMS")
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
            Label("VITA SHORTCUTS")
            Text(
                "Shortcut output directory",
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
                Text("SELECT OUTPUT FOLDER")
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
                                            contentDescription = "Delete shortcut for ${app.title}",
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
internal fun OrganizerPresetCard(
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
internal fun NszKeysCard(
    configured: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("NSZ keys", style = MaterialTheme.typography.titleMedium)
            Text(
                if (configured) {
                    "prod.keys is imported and will be kept in Mimir's private storage."
                } else {
                    "Import your legally obtained prod.keys file before scanning or converting NSZ packages."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )
            Button(onClick = onSelect, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(if (configured) "REPLACE PROD.KEYS" else "IMPORT PROD.KEYS")
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
                    "System",
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
                        "Conversion type",
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
                        title = "Compatibility guidance",
                        body = "Use CD when using NetherSX2; use DVD when using ARMSX2.",
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
