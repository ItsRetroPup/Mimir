package pup.app.mimir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import pup.app.mimir.domain.OperationPlan
import pup.app.mimir.domain.PlannedChange
import pup.app.mimir.domain.ToolMode

/**
 * One planned change for the organiser and zipper: which files it reads and what it writes, so
 * nothing on the card is a surprise when the change is applied.
 */
@Composable
internal fun ChangeRow(
    plan: OperationPlan,
    change: PlannedChange,
    selected: Boolean,
    enabled: Boolean,
    largest: Long,
    onSelectedChange: (Boolean) -> Unit,
) {
    val palette = LocalMimirPalette.current
    val ink = MaterialTheme.colorScheme.primary
    var showOverwriteWarning by rememberSaveable(change.detailPath) { mutableStateOf(false) }
    val badge = when (plan.mode) {
        ToolMode.MultiDiscOrganizer -> pluralStringResource(R.plurals.chip_discs, change.sourceFiles.size, change.sourceFiles.size)
        else -> change.detailPath.substringAfterLast('.', "").uppercase()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) ink.copy(alpha = 0.1f) else palette.panel)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(
                checked = selected,
                onCheckedChange = { shouldSelect ->
                    if (shouldSelect && change.targetAlreadyExists && !selected) showOverwriteWarning = true
                    else onSelectedChange(shouldSelect)
                },
                enabled = enabled,
            )
            Text(
                change.title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (badge.isNotEmpty()) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(ink.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        SizeBar(change.sourceSizeBytes, largest, Modifier.padding(start = 48.dp))
        if (change.targetAlreadyExists) {
            Text(
                stringResource(R.string.preview_existing_output, change.detailPath.substringAfterLast('.')),
                style = MaterialTheme.typography.bodySmall,
                color = palette.caution,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            FileList(stringResource(R.string.preview_sources), change.sourceFiles, Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.padding(top = 18.dp).size(18.dp),
            )
            FileList(stringResource(R.string.preview_targets), change.targetFiles, Modifier.weight(1f))
        }
    }
    if (showOverwriteWarning) {
        AlertDialog(
            onDismissRequest = { showOverwriteWarning = false },
            title = { Text(stringResource(R.string.replace_output_title)) },
            text = { Text(stringResource(R.string.replace_output_body_convert, change.targetFiles.singleOrNull() ?: change.detailPath)) },
            confirmButton = {
                InkButton(
                    onClick = {
                        showOverwriteWarning = false
                        onSelectedChange(true)
                    },
                    containerColor = palette.caution,
                    contentColor = palette.onCaution,
                ) { Text(stringResource(R.string.action_replace_output)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showOverwriteWarning = false }, modifier = Modifier.focusRing()) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun FileList(title: String, paths: List<String>, modifier: Modifier) {
    val palette = LocalMimirPalette.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = palette.textMuted)
        paths.forEach { path ->
            Text(path, style = MaterialTheme.typography.bodySmall, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
