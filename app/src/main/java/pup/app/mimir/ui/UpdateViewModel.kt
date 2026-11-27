package pup.app.mimir.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pup.app.mimir.data.UpdateRepository
import pup.app.mimir.domain.AvailableUpdate
import pup.app.mimir.domain.UpdatePolicy

/** What the update dialog shows. [Hidden] means no dialog. */
sealed interface UpdateState {
    data object Hidden : UpdateState
    data class Available(val update: AvailableUpdate) : UpdateState
    /** [progress] is 0..1, or null while the size is unknown. */
    data class Downloading(val update: AvailableUpdate, val progress: Float?) : UpdateState
    /** Downloaded and checked; the installer has been opened and can be opened again. */
    data class ReadyToInstall(val update: AvailableUpdate, val apk: File) : UpdateState
    data class Failed(val update: AvailableUpdate, val reason: UpdateFailure) : UpdateState
}

enum class UpdateFailure {
    /** The network dropped or the file arrived damaged. */
    Download,

    /** The APK is signed with a different key, so Android would refuse to install it over this one. */
    SignatureMismatch,
}

/**
 * Checks for a newer release once each time Mimir starts and drives the update dialog.
 * It is kept apart from [MimirViewModel] because it shares nothing with the tools.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UpdateRepository(application)
    private val prefs = application.getSharedPreferences("mimir_prefs", 0)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Hidden)
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private var downloadJob: Job? = null

    init {
        // A debug build is signed with the debug key, so a release could never install over it.
        if (!repository.isDebugBuild()) checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            // An offline device or a GitHub hiccup just means no dialog this time.
            val latest = runCatching { repository.fetchLatest() }
                .onFailure { Log.w(TAG, "Update check failed", it) }
                .getOrNull() ?: return@launch
            val skipped = prefs.getString(KEY_SKIPPED_VERSION, null)
            if (UpdatePolicy.shouldOffer(repository.installedVersion(), latest.version, skipped)) {
                _state.value = UpdateState.Available(latest)
            }
        }
    }

    /** "Skip": hide the dialog until Mimir is next opened. */
    fun dismiss() {
        downloadJob?.cancel()
        _state.value = UpdateState.Hidden
    }

    /** "Skip this version": never offer this version again; a newer one is still offered. */
    fun skipThisVersion() {
        val update = currentUpdate() ?: return
        prefs.edit().putString(KEY_SKIPPED_VERSION, update.version.toString()).apply()
        dismiss()
    }

    fun download() {
        val update = currentUpdate() ?: return
        if (downloadJob?.isActive == true) return
        _state.value = UpdateState.Downloading(update, progress = null)
        downloadJob = viewModelScope.launch {
            val apk = runCatching {
                repository.download(update) { progress ->
                    _state.value = UpdateState.Downloading(update, progress)
                }
            }.onFailure { Log.w(TAG, "Update download failed", it) }.getOrNull()
            _state.value = when {
                apk == null -> UpdateState.Failed(update, UpdateFailure.Download)
                !repository.isSignedLikeInstalledApp(apk) -> {
                    apk.delete()
                    UpdateState.Failed(update, UpdateFailure.SignatureMismatch)
                }
                else -> UpdateState.ReadyToInstall(update, apk).also { install() }
            }
        }
    }

    /** Opens Android's installer. Installing replaces Mimir, so Android closes it and reopens it updated. */
    fun install() {
        val ready = _state.value as? UpdateState.ReadyToInstall ?: return
        runCatching { repository.launchInstaller(ready.apk) }
            .onFailure {
                Log.w(TAG, "Couldn't open the installer", it)
                _state.value = UpdateState.Failed(ready.update, UpdateFailure.Download)
            }
    }

    private fun currentUpdate(): AvailableUpdate? = when (val state = _state.value) {
        is UpdateState.Available -> state.update
        is UpdateState.Downloading -> state.update
        is UpdateState.ReadyToInstall -> state.update
        is UpdateState.Failed -> state.update
        UpdateState.Hidden -> null
    }

    private companion object {
        const val TAG = "MimirUpdate"
        const val KEY_SKIPPED_VERSION = "update_skipped_version"
    }
}
