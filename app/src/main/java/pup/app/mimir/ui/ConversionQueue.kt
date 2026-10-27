package pup.app.mimir.ui

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
import androidx.compose.ui.unit.dp
import pup.app.mimir.domain.OperationPlan

internal enum class QueueSortOption(val label: String) {
    NameAscending("Name A–Z"),
    NameDescending("Name Z–A"),
    SizeLargest("Size largest"),
    SizeSmallest("Size smallest"),
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
                Label("CONVERSION QUEUE")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onSelectAll, enabled = enabled) { Text("SELECT ALL") }
                    OutlinedButton(onClick = onDeselectAll, enabled = enabled) { Text("DESELECT ALL") }
                }
            }
            Text("Sort by", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            ChoiceRow(
                options = QueueSortOption.entries,
                selected = sortOption,
                enabled = enabled,
                label = { it.label },
                onSelect = onSortOptionChanged,
            )
            if (storageInfo != null) {
                InfoPanel(
                    title = "Storage on scanned device",
                    body = "${formatBytes(storageInfo.freeBytes)} free of ${formatBytes(storageInfo.totalBytes)}. " +
                        "This queue needs about ${formatBytes(selectedBytes)} of working space.",
                    accent = if (lowSpace) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                )
                if (lowSpace) {
                    Text(
                        "Warning: there may not be enough free space to convert the selected queue. Deselect some items or free space first.",
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
                Text("ROM", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelMedium)
                Text("SIZE", modifier = Modifier.weight(0.65f), style = MaterialTheme.typography.labelMedium)
                Text("OUTPUT", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
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
                            Text("Existing output — select individually to replace", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
            title = { Text("Replace existing output?") },
            text = { Text("${change.targetFiles.singleOrNull() ?: change.detailPath} already exists and will be overwritten if selected.") },
            confirmButton = {
                Button(onClick = {
                    pendingOverwrite = null
                    onChangeSelection(change.detailPath, true)
                }) { Text("REPLACE OUTPUT") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingOverwrite = null }) { Text("CANCEL") }
            },
        )
    }
}

@Composable
internal fun ConversionReportCard(report: pup.app.mimir.ui.ConversionReport) {
    val sizeSummary = when {
        report.spaceSavedBytes > 0L -> "Net space saved: ${formatBytes(report.spaceSavedBytes)}."
        report.spaceSavedBytes < 0L -> "Net size increase: ${formatBytes(-report.spaceSavedBytes)}."
        else -> "No net size change."
    }
    InfoPanel(
        title = "Conversion report",
        body = "Completed ${report.completed} of ${report.total} conversions. " +
            sizeSummary +
            if (report.stopped) " Stopped by user." else "",
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
                "Converting ${currentJobLabel ?: "selected image"}: ${(currentJobProgress * 100).toInt()}%",
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
                    "Queue: job $currentJob of ${report.total}",
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
            ) { Text("STOP AFTER CURRENT CONVERSION") }
            OutlinedButton(
                onClick = onStopNow,
                enabled = stopNowEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("STOP NOW") }
        }
    }
}
