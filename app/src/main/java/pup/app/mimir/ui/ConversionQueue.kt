package pup.app.mimir.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pup.app.mimir.R
import pup.app.mimir.domain.OperationPlan

internal enum class QueueSortOption(@StringRes val label: Int) {
    NameAscending(R.string.sort_name_ascending),
    NameDescending(R.string.sort_name_descending),
    SizeLargest(R.string.sort_size_largest),
    SizeSmallest(R.string.sort_size_smallest),
}

internal fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "%.1f %s".format(java.util.Locale.US, value, units[unit])
}

internal fun formatQueueSize(bytes: Long): String {
    val gibibyte = 1024L * 1024L * 1024L
    return if (bytes < gibibyte) {
        "%.1f MB".format(java.util.Locale.US, bytes / (1024.0 * 1024.0))
    } else {
        "%.1f GB".format(java.util.Locale.US, bytes / gibibyte.toDouble())
    }
}

@Composable
internal fun ConversionQueueTable(
    plan: OperationPlan,
    selectedPaths: Set<String>,
    sortOption: QueueSortOption,
    enabled: Boolean,
    storageInfo: pup.app.mimir.data.RomTreeRepository.StorageInfo?,
    onSortOptionChanged: (QueueSortOption) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onChangeSelection: (String, Boolean) -> Unit,
) {
    var pendingOverwrite by remember { mutableStateOf<pup.app.mimir.domain.PlannedChange?>(null) }
    val sortedChanges = when (sortOption) {
        QueueSortOption.NameAscending -> plan.changes.sortedBy { it.title.lowercase() }
        QueueSortOption.NameDescending -> plan.changes.sortedByDescending { it.title.lowercase() }
        QueueSortOption.SizeLargest -> plan.changes.sortedByDescending { it.sourceSizeBytes }
        QueueSortOption.SizeSmallest -> plan.changes.sortedBy { it.sourceSizeBytes }
    }
    val selectedBytes = plan.changes
        .filter { it.detailPath in selectedPaths }
        .sumOf { it.sourceSizeBytes }
    val lowSpace = storageInfo != null && selectedBytes > storageInfo.freeBytes

    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Label(stringResource(R.string.queue_title))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onSelectAll, enabled = enabled) { Text(stringResource(R.string.action_select_all)) }
                    OutlinedButton(onClick = onDeselectAll, enabled = enabled) { Text(stringResource(R.string.action_deselect_all)) }
                }
            }
            Text(stringResource(R.string.queue_sort_by), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            ChoiceRow(
                options = QueueSortOption.entries,
                selected = sortOption,
                enabled = enabled,
                label = { stringResource(it.label) },
                onSelect = onSortOptionChanged,
            )
            if (storageInfo != null) {
                InfoPanel(
                    title = stringResource(R.string.storage_title),
                    body = stringResource(
                        R.string.storage_body,
                        formatBytes(storageInfo.freeBytes),
                        formatBytes(storageInfo.totalBytes),
                        formatBytes(selectedBytes),
                    ),
                    accent = if (lowSpace) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                )
                if (lowSpace) {
                    Text(
                        stringResource(R.string.storage_low_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("", modifier = Modifier.size(40.dp))
                Text(stringResource(R.string.queue_column_rom), modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.queue_column_size), modifier = Modifier.weight(0.65f), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.queue_column_output), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            }
            sortedChanges.forEach { change ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = change.detailPath in selectedPaths,
                        onCheckedChange = { checked ->
                            if (checked && change.targetAlreadyExists) {
                                pendingOverwrite = change
                            } else {
                                onChangeSelection(change.detailPath, checked)
                            }
                        },
                        enabled = enabled,
                    )
                    Column(modifier = Modifier.weight(1.4f)) {
                        Text(change.title, style = MaterialTheme.typography.bodyMedium)
                        if (change.targetAlreadyExists) {
                            Text(stringResource(R.string.queue_existing_output), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Text(formatBytes(change.sourceSizeBytes), modifier = Modifier.weight(0.65f), style = MaterialTheme.typography.bodySmall)
                    Text(change.targetFiles.singleOrNull().orEmpty(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    pendingOverwrite?.let { change ->
        AlertDialog(
            onDismissRequest = { pendingOverwrite = null },
            title = { Text(stringResource(R.string.replace_output_title)) },
            text = { Text(stringResource(R.string.replace_output_body_selected, change.targetFiles.singleOrNull() ?: change.detailPath)) },
            confirmButton = {
                Button(onClick = {
                    pendingOverwrite = null
                    onChangeSelection(change.detailPath, true)
                }) { Text(stringResource(R.string.action_replace_output)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingOverwrite = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
internal fun ConversionReportCard(report: pup.app.mimir.ui.ConversionReport) {
    val sizeSummary = when {
        report.spaceSavedBytes > 0L -> stringResource(R.string.report_space_saved, formatBytes(report.spaceSavedBytes))
        report.spaceSavedBytes < 0L -> stringResource(R.string.report_size_increase, formatBytes(-report.spaceSavedBytes))
        else -> stringResource(R.string.report_no_size_change)
    }
    InfoPanel(
        title = stringResource(R.string.report_title),
        body = listOfNotNull(
            pluralStringResource(R.plurals.report_completed, report.total, report.completed, report.total),
            sizeSummary,
            stringResource(R.string.report_stopped).takeIf { report.stopped },
        ).joinToString(" "),
        accent = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
internal fun ConversionProgressToast(
    report: pup.app.mimir.ui.ConversionReport,
    currentJobLabel: String?,
    currentJobProgress: Float,
    canStopAfterCurrent: Boolean,
    stopNowEnabled: Boolean,
    onStopNow: () -> Unit,
    onStopAfterCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentJob = (report.completed + 1).coerceAtMost(report.total)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(
                    R.string.progress_converting,
                    currentJobLabel ?: stringResource(R.string.progress_selected_image),
                    (currentJobProgress * 100).toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { currentJobProgress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            if (report.total > 1) {
                Text(
                    stringResource(R.string.progress_queue, currentJob, report.total),
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { (report.completed + currentJobProgress) / report.total },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onStopAfterCurrent,
                enabled = canStopAfterCurrent,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_stop_after_current)) }
            OutlinedButton(
                onClick = onStopNow,
                enabled = stopNowEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_stop_now)) }
        }
    }
}
