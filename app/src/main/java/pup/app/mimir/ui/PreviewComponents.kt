package pup.app.mimir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pup.app.mimir.R
import pup.app.mimir.domain.ConverterTool
import pup.app.mimir.domain.FileOperation
import pup.app.mimir.domain.OperationPlan
import pup.app.mimir.domain.ToolMode

@Composable
internal fun ChangeCard(
    plan: OperationPlan,
    change: pup.app.mimir.domain.PlannedChange,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSelectedChange: (Boolean) -> Unit,
) {
    var showOverwriteWarning by rememberSaveable(change.detailPath) { mutableStateOf(false) }
    val chipText = when (plan.mode) {
        ToolMode.MultiDiscOrganizer -> pluralStringResource(R.plurals.chip_discs, change.sourceFiles.size, change.sourceFiles.size)
        ToolMode.RomZipper -> "ZIP"
        ToolMode.Converter -> change.operations
            .firstNotNullOfOrNull { (it as? FileOperation.ConvertWithTool)?.tool?.badge }
            ?: ConverterTool.Chd.badge
        ToolMode.VitaAppIds -> "VITA"
        ToolMode.EsDeSystems -> "ES-DE"
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        shape = RoundedCornerShape(12.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { shouldSelect ->
                            if (shouldSelect && change.targetAlreadyExists && !selected) {
                                showOverwriteWarning = true
                            } else {
                                onSelectedChange(shouldSelect)
                            }
                        },
                        enabled = enabled,
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        val icon = when (plan.mode) {
                            ToolMode.RomZipper -> Icons.Outlined.FolderZip
                            ToolMode.Converter -> Icons.Outlined.Archive
                            ToolMode.VitaAppIds -> Icons.Outlined.Home
                            ToolMode.EsDeSystems -> Icons.Outlined.Folder
                            else -> Icons.Outlined.Folder
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (change.targetAlreadyExists) {
                Text(
                    stringResource(R.string.preview_existing_output, change.detailPath.substringAfterLast('.')),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.preview_sources), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                change.sourceFiles.forEach { path ->
                    Text(path, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.preview_targets), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                change.targetFiles.forEach { path ->
                    Text(path, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    if (showOverwriteWarning) {
        AlertDialog(
            onDismissRequest = { showOverwriteWarning = false },
            title = { Text(stringResource(R.string.replace_output_title)) },
            text = { Text(stringResource(R.string.replace_output_body_convert, change.targetFiles.singleOrNull() ?: change.detailPath)) },
            confirmButton = {
                Button(onClick = {
                    showOverwriteWarning = false
                    onSelectedChange(true)
                }) { Text(stringResource(R.string.action_replace_output)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showOverwriteWarning = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
internal fun PreviewSummary(
    previewCount: Int,
    selectedCount: Int,
    selectedPaths: Set<String>,
    plan: OperationPlan,
    onApply: () -> Unit,
    isBusy: Boolean,
) {
    val selectedSize = plan.changes
        .filter { it.detailPath in selectedPaths }
        .sumOf { it.sourceSizeBytes }
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label(stringResource(R.string.preview_title))
            if (plan.mode == ToolMode.Converter) {
                Text(
                    pluralStringResource(R.plurals.preview_jobs_selected, selectedCount, selectedCount, formatQueueSize(selectedSize)),
                    style = MaterialTheme.typography.headlineMedium,
                )
            } else {
                Text(
                    when (plan.mode) {
                        ToolMode.MultiDiscOrganizer -> R.plurals.preview_multidisc_summary
                        ToolMode.RomZipper -> R.plurals.preview_zip_summary
                        ToolMode.Converter, ToolMode.VitaAppIds -> R.plurals.preview_items_summary
                        ToolMode.EsDeSystems -> R.plurals.preview_systems_summary
                    }.let { pluralStringResource(it, previewCount, previewCount, selectedCount) },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Button(
                    onClick = onApply,
                    enabled = !isBusy && selectedCount > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_apply))
                }
            }
        }
    }
}
