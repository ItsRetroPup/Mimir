package pup.app.mimir.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pup.app.mimir.R
import pup.app.mimir.domain.ConverterTool
import pup.app.mimir.domain.ToolMode
import pup.app.mimir.domain.VitaApp
import pup.app.mimir.domain.VitaShortcutFormat

/** Confirms applying the selected preview changes; [onConfirm] receives the CHD delete-originals choice. */
@Composable
internal fun ApplyConfirmationDialog(
    mode: ToolMode,
    converterTool: ConverterTool,
    changeCount: Int,
    existingOutputCount: Int,
    deleteOriginalChdFiles: Boolean,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (deleteOriginalChdFiles: Boolean) -> Unit,
) {
    val palette = LocalMimirPalette.current
    val isConversion = mode == ToolMode.Converter
    val offersDeleteOriginals = isConversion && converterTool == ConverterTool.Chd
    var deleteOriginals by rememberSaveable { mutableStateOf(deleteOriginalChdFiles) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (isConversion) R.string.apply_title_convert else R.string.apply_title_changes))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    when {
                        !isConversion -> pluralStringResource(R.plurals.apply_body_changes, changeCount, changeCount)
                        converterTool.replacesSource -> pluralStringResource(R.plurals.apply_body_replace_xiso, changeCount, changeCount)
                        else -> listOfNotNull(
                            pluralStringResource(R.plurals.apply_body_convert, changeCount, changeCount, converterTool.outputExtension),
                            pluralStringResource(R.plurals.apply_body_overwrite, existingOutputCount, existingOutputCount)
                                .takeIf { existingOutputCount > 0 },
                            stringResource(R.string.apply_body_cannot_undo),
                        ).joinToString(" ")
                    }
                )
                if (offersDeleteOriginals) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.apply_delete_originals), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.apply_delete_originals_detail),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                        Switch(checked = deleteOriginals, onCheckedChange = { deleteOriginals = it })
                    }
                }
            }
        },
        confirmButton = {
            InkButton(
                onClick = { onConfirm(deleteOriginals) },
                enabled = enabled && changeCount > 0,
                containerColor = palette.caution,
                contentColor = palette.onCaution,
            ) {
                Text(stringResource(if (isConversion) R.string.action_start_conversions else R.string.action_apply_changes))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.focusRing()) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
internal fun EsDeInstallDialog(
    systemCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.esde_install_title)) },
        text = { Text(pluralStringResource(R.plurals.esde_install_body, systemCount, systemCount)) },
        confirmButton = {
            InkButton(onClick = onConfirm) { Text(stringResource(R.string.action_install_systems)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.focusRing()) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
internal fun VitaShortcutAddDialog(
    app: VitaApp,
    format: VitaShortcutFormat,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vita_add_title)) },
        text = { Text(stringResource(R.string.vita_add_body, format.extension, app.title, app.titleId)) },
        confirmButton = {
            InkButton(onClick = onConfirm) { Text(stringResource(R.string.action_create_shortcut)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.focusRing()) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
internal fun VitaShortcutRemoveDialog(
    app: VitaApp,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vita_remove_title)) },
        text = { Text(stringResource(R.string.vita_remove_body, app.title, app.titleId)) },
        confirmButton = {
            InkButton(
                onClick = onConfirm,
                containerColor = LocalMimirPalette.current.caution,
                contentColor = LocalMimirPalette.current.onCaution,
            ) { Text(stringResource(R.string.action_remove_shortcut)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.focusRing()) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** About Mimir, opened from the footer link on home: who makes it and how to support it. */
@Composable
internal fun AboutDialog(onOpenUrl: (String) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalMimirPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { MimirMark(Modifier.size(48.dp)) },
        title = { Text(stringResource(R.string.about_mimir)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.support_body), style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
                SupportLink("YouTube", Icons.Outlined.Subscriptions) { onOpenUrl(YOUTUBE_CHANNEL_URL) }
                SupportLink("Ko-fi", Icons.Outlined.Favorite) { onOpenUrl(KOFI_URL) }
                SupportLink("Buy Me a Coffee", Icons.Outlined.Coffee) { onOpenUrl(BUY_ME_A_COFFEE_URL) }
                Text(stringResource(R.string.home_footer), style = MaterialTheme.typography.labelMedium, color = palette.textMuted)
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.focusRing()) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun SupportLink(label: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.focusRing(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.size(10.dp))
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge)
    }
}
