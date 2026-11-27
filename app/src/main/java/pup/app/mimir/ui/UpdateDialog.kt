package pup.app.mimir.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import pup.app.mimir.R
import pup.app.mimir.domain.AvailableUpdate

/** The update prompt: what's new, then Skip / Skip this version / Download, then download progress. */
@Composable
internal fun UpdateDialog(
    state: UpdateState,
    onSkip: () -> Unit,
    onSkipThisVersion: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val update = when (state) {
        is UpdateState.Available -> state.update
        is UpdateState.Downloading -> state.update
        is UpdateState.ReadyToInstall -> state.update
        is UpdateState.Failed -> state.update
        UpdateState.Hidden -> return
    }
    val context = LocalContext.current
    val downloading = state is UpdateState.Downloading
    AlertDialog(
        // A tap outside shouldn't drop a download halfway; Skip or Cancel does that on purpose.
        onDismissRequest = { if (!downloading) onSkip() },
        properties = DialogProperties(dismissOnClickOutside = !downloading),
        title = { Text(stringResource(R.string.update_title, update.version.toString())) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state) {
                    is UpdateState.Downloading -> DownloadProgress(state.progress)
                    is UpdateState.ReadyToInstall -> Text(stringResource(R.string.update_ready_body))
                    is UpdateState.Failed -> Text(
                        stringResource(
                            when (state.reason) {
                                UpdateFailure.Download -> R.string.update_failed_download
                                UpdateFailure.SignatureMismatch -> R.string.update_failed_signature
                            },
                        ),
                    )
                    else -> WhatsNew(update)
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Available -> InkButton(onClick = onDownload) { Text(stringResource(R.string.update_action_download)) }
                is UpdateState.Downloading -> InkButton(onClick = {}, enabled = false) { Text(stringResource(R.string.update_action_download)) }
                is UpdateState.ReadyToInstall -> InkButton(onClick = onInstall) { Text(stringResource(R.string.update_action_install)) }
                is UpdateState.Failed -> when (state.reason) {
                    UpdateFailure.Download -> InkButton(onClick = onDownload) { Text(stringResource(R.string.update_action_retry)) }
                    // The installer can't help here, so send them to the release page to reinstall by hand.
                    UpdateFailure.SignatureMismatch -> InkButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releasePageUrl)))
                            onSkip()
                        },
                    ) { Text(stringResource(R.string.update_action_open_release)) }
                }
                UpdateState.Hidden -> Unit
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state is UpdateState.Available) {
                    OutlinedButton(onClick = onSkipThisVersion, modifier = Modifier.focusRing()) {
                        Text(stringResource(R.string.update_action_skip_version))
                    }
                }
                OutlinedButton(onClick = onSkip, modifier = Modifier.focusRing()) {
                    Text(stringResource(if (downloading) R.string.action_cancel else R.string.update_action_skip))
                }
            }
        },
    )
}

@Composable
private fun WhatsNew(update: AvailableUpdate) {
    if (update.highlights.isEmpty()) {
        Text(stringResource(R.string.update_body_no_notes))
        return
    }
    Text(stringResource(R.string.update_whats_new), style = MaterialTheme.typography.titleSmall)
    // Long notes scroll inside the dialog so the buttons stay on screen.
    Column(
        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        update.highlights.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•")
                Text(item, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DownloadProgress(progress: Float?) {
    Text(
        if (progress == null) {
            stringResource(R.string.update_downloading)
        } else {
            stringResource(R.string.update_downloading_percent, (progress * 100).toInt())
        },
    )
    if (progress == null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}
