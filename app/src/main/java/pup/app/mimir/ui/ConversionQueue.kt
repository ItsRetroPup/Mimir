package pup.app.mimir.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pup.app.mimir.R
import pup.app.mimir.data.RomTreeRepository
import pup.app.mimir.domain.OperationPlan
import pup.app.mimir.domain.PlannedChange
import pup.app.mimir.domain.ToolMode

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

/**
 * Progress drawn as the memory card's copy bar: a row of blocks filling left to right. A null
 * [progress] sweeps a lit group of blocks back and forth while the amount of work is unknown.
 */
@Composable
internal fun BlockProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    ink: Color = MaterialTheme.colorScheme.primary,
    blocks: Int = 24,
    height: Dp = 12.dp,
) {
    val palette = LocalMimirPalette.current
    val sweep = if (progress == null && !reduceMotion()) {
        rememberInfiniteTransition(label = "blockSweep").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
            label = "blockSweepPosition",
        ).value
    } else {
        0f
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress ?: 0f, 0f..1f) },
    ) {
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (blocks - 1)) / blocks
        val lit = progress?.let { it * blocks }
        repeat(blocks) { i ->
            val on = if (lit != null) {
                (lit - i).coerceIn(0f, 1f)
            } else {
                val centre = sweep * (blocks - 4) + 2
                if (kotlin.math.abs(i - centre) < 2.5f) 1f else 0f
            }
            val x = i * (w + gap)
            drawRoundRect(palette.gridLine, Offset(x, 0f), Size(w, size.height), CornerRadius(2.dp.toPx()))
            if (on > 0f) {
                drawRoundRect(ink.copy(alpha = 0.35f + 0.65f * on), Offset(x, 0f), Size(w * on.coerceAtLeast(0.15f), size.height), CornerRadius(2.dp.toPx()))
            }
        }
    }
}

/** What the scan found, and the controls for choosing which of it to act on. */
@Composable
internal fun PlanHeader(
    plan: OperationPlan,
    previewCount: Int,
    selectedPaths: Set<String>,
    storageInfo: RomTreeRepository.StorageInfo?,
    sortOption: QueueSortOption,
    enabled: Boolean,
    onSortOptionChanged: (QueueSortOption) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
) {
    val palette = LocalMimirPalette.current
    val selectedCount = selectedPaths.size
    val selectedBytes = plan.changes.filter { it.detailPath in selectedPaths }.sumOf { it.sourceSizeBytes }
    val isConversion = plan.mode == ToolMode.Converter
    Text(
        if (isConversion) {
            pluralStringResource(R.plurals.preview_jobs_selected, selectedCount, selectedCount, formatQueueSize(selectedBytes))
        } else {
            val summary = when (plan.mode) {
                ToolMode.MultiDiscOrganizer -> R.plurals.preview_multidisc_summary
                ToolMode.RomZipper -> R.plurals.preview_zip_summary
                ToolMode.Converter, ToolMode.VitaAppIds -> R.plurals.preview_items_summary
                ToolMode.EsDeSystems -> R.plurals.preview_systems_summary
                ToolMode.RomPatcher -> R.plurals.preview_items_summary
            }
            pluralStringResource(summary, previewCount, previewCount, selectedCount)
        },
        style = MaterialTheme.typography.headlineSmall,
        color = palette.text,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onSelectAll, enabled = enabled, modifier = Modifier.focusRing()) { Text(stringResource(R.string.action_select_all)) }
        OutlinedButton(onClick = onDeselectAll, enabled = enabled, modifier = Modifier.focusRing()) { Text(stringResource(R.string.action_deselect_all)) }
    }
    if (isConversion) {
        ChoiceRow(
            options = QueueSortOption.entries,
            selected = sortOption,
            enabled = enabled,
            label = { stringResource(it.label) },
            onSelect = onSortOptionChanged,
        )
        if (storageInfo != null) {
            val lowSpace = selectedBytes > storageInfo.freeBytes
            Text(
                stringResource(
                    R.string.storage_body,
                    formatBytes(storageInfo.freeBytes),
                    formatBytes(storageInfo.totalBytes),
                    formatBytes(selectedBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted,
            )
            if (lowSpace) {
                StatusLine(StatusMessage(stringResource(R.string.storage_low_warning), isError = true))
            }
        }
    }
}

/** The conversion queue as memory-card file rows: name, size, and the file it will write. */
@Composable
internal fun ConversionQueueList(
    plan: OperationPlan,
    selectedPaths: Set<String>,
    sortOption: QueueSortOption,
    enabled: Boolean,
    onChangeSelection: (String, Boolean) -> Unit,
) {
    val palette = LocalMimirPalette.current
    var pendingOverwrite by remember { mutableStateOf<PlannedChange?>(null) }
    val sortedChanges = when (sortOption) {
        QueueSortOption.NameAscending -> plan.changes.sortedBy { it.title.lowercase() }
        QueueSortOption.NameDescending -> plan.changes.sortedByDescending { it.title.lowercase() }
        QueueSortOption.SizeLargest -> plan.changes.sortedByDescending { it.sourceSizeBytes }
        QueueSortOption.SizeSmallest -> plan.changes.sortedBy { it.sourceSizeBytes }
    }
    val largest = plan.changes.maxOfOrNull { it.sourceSizeBytes }?.coerceAtLeast(1L) ?: 1L
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sortedChanges.forEach { change ->
            val checked = change.detailPath in selectedPaths
            DataRow(selected = checked) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { wantChecked ->
                        if (wantChecked && change.targetAlreadyExists) pendingOverwrite = change
                        else onChangeSelection(change.detailPath, wantChecked)
                    },
                    enabled = enabled,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(change.title, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    SizeBar(change.sourceSizeBytes, largest)
                    Text(
                        change.targetFiles.singleOrNull().orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (change.targetAlreadyExists) {
                        Text(stringResource(R.string.queue_existing_output), style = MaterialTheme.typography.bodySmall, color = palette.caution)
                    }
                }
                Text(
                    formatBytes(change.sourceSizeBytes),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = palette.textMuted,
                )
            }
        }
    }
    pendingOverwrite?.let { change ->
        AlertDialog(
            onDismissRequest = { pendingOverwrite = null },
            title = { Text(stringResource(R.string.replace_output_title)) },
            text = { Text(stringResource(R.string.replace_output_body_selected, change.targetFiles.singleOrNull() ?: change.detailPath)) },
            confirmButton = {
                InkButton(
                    onClick = {
                        pendingOverwrite = null
                        onChangeSelection(change.detailPath, true)
                    },
                    containerColor = palette.caution,
                    contentColor = palette.onCaution,
                ) { Text(stringResource(R.string.action_replace_output)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingOverwrite = null }, modifier = Modifier.focusRing()) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
internal fun ConversionReportPanel(report: ConversionReport) {
    val palette = LocalMimirPalette.current
    val sizeSummary = when {
        report.spaceSavedBytes > 0L -> stringResource(R.string.report_space_saved, formatBytes(report.spaceSavedBytes))
        report.spaceSavedBytes < 0L -> stringResource(R.string.report_size_increase, formatBytes(-report.spaceSavedBytes))
        else -> stringResource(R.string.report_no_size_change)
    }
    ConsolePanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        Text(stringResource(R.string.report_title), style = MaterialTheme.typography.titleLarge, color = palette.text)
        Text(
            listOfNotNull(
                pluralStringResource(R.plurals.report_completed, report.total, report.completed, report.total),
                sizeSummary,
                stringResource(R.string.report_stopped).takeIf { report.stopped },
            ).joinToString(" "),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted,
        )
    }
}

/** The running conversion, shown in place at the top of the data pane like a memory card copy. */
@Composable
internal fun ConversionProgressPanel(
    report: ConversionReport,
    currentJobLabel: String?,
    currentJobProgress: Float,
    canStopAfterCurrent: Boolean,
    stopNowEnabled: Boolean,
    onStopNow: () -> Unit,
    onStopAfterCurrent: () -> Unit,
) {
    val palette = LocalMimirPalette.current
    val currentJob = (report.completed + 1).coerceAtMost(report.total)
    ConsolePanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        Text(
            stringResource(
                R.string.progress_converting,
                currentJobLabel ?: stringResource(R.string.progress_selected_image),
                (currentJobProgress * 100).toInt(),
            ),
            style = MaterialTheme.typography.titleMedium,
            color = palette.text,
        )
        BlockProgressBar(currentJobProgress)
        if (report.total > 1) {
            Text(
                stringResource(R.string.progress_queue, currentJob, report.total),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = palette.textMuted,
            )
            BlockProgressBar((report.completed + currentJobProgress) / report.total, ink = palette.textMuted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onStopAfterCurrent, enabled = canStopAfterCurrent, modifier = Modifier.weight(1f).focusRing()) {
                Text(stringResource(R.string.action_stop_after_current), maxLines = 2)
            }
            InkButton(
                onClick = onStopNow,
                enabled = stopNowEnabled,
                modifier = Modifier.weight(1f),
                containerColor = palette.caution,
                contentColor = palette.onCaution,
            ) { Text(stringResource(R.string.action_stop_now), maxLines = 2) }
        }
    }
}

/** A scan or an apply in progress, with its live label. */
@Composable
internal fun ProgressPanel(label: String, progress: Float?) {
    val palette = LocalMimirPalette.current
    ConsolePanel(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = palette.text)
        BlockProgressBar(progress)
    }
}

/** An item's size as a short run of blocks, relative to the largest item in the list. */
@Composable
internal fun SizeBar(bytes: Long, largest: Long, modifier: Modifier = Modifier) {
    if (bytes <= 0L) return
    BlockProgressBar(
        progress = (bytes.toFloat() / largest).coerceIn(0.04f, 1f),
        modifier = modifier.width(160.dp),
        blocks = 16,
        height = 5.dp,
    )
}
