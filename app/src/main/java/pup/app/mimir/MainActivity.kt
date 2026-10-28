package pup.app.mimir

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pup.app.mimir.ui.MimirScreen
import pup.app.mimir.ui.MimirTheme
import pup.app.mimir.ui.MimirViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MimirViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            // System bar icons follow Mimir's own theme toggle, not the system night mode.
            LaunchedEffect(uiState.useDarkMode) {
                val style = if (uiState.useDarkMode) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            var pendingEsDeSystem by remember { mutableStateOf<String?>(null) }
            val folderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                viewModel.onFolderSelected(uri)
            }
            val vitaOutputLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                viewModel.onVitaOutputSelected(uri)
            }
            val nszKeysLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                viewModel.onNszKeysSelected(uri)
            }
            val esDeFolderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                viewModel.onEsDeFolderSelected(uri)
            }
            val esDeSystemFolderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                val systemName = pendingEsDeSystem
                pendingEsDeSystem = null
                if (uri != null && systemName != null) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    viewModel.onEsDeSystemFolderSelected(systemName, uri)
                }
            }
            val patchRomLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                viewModel.onPatchRomSelected(uri)
            }
            val patchFileLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                viewModel.onPatchFileSelected(uri)
            }
            // Asked for only when no granted folder holds the picked ROM, so the copy can be saved beside it.
            val patchFolderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                viewModel.onPatchFolderGranted(uri)
            }
            MimirTheme(useDarkMode = uiState.useDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MimirScreen(
                        uiState = uiState,
                        onSelectFolder = { folderLauncher.launch(null) },
                        onSelectVitaOutput = { vitaOutputLauncher.launch(null) },
                        onSelectNszKeys = { nszKeysLauncher.launch(arrayOf("*/*")) },
                        onSelectEsDeFolder = { esDeFolderLauncher.launch(null) },
                        onRefreshEsDeSystems = viewModel::refreshEsDeSystems,
                        onApplyEsDeSystems = viewModel::applyEsDeSystems,
                        onPickEsDeSystemFolder = { systemName ->
                            pendingEsDeSystem = systemName
                            esDeSystemFolderLauncher.launch(viewModel.defaultEsDeFolderUri(systemName))
                        },
                        onModeSelected = viewModel::updateMode,
                        onPresetSelected = viewModel::updatePreset,
                        onChdSystemSelected = viewModel::updateChdSystem,
                        onChdDiscTypeSelected = viewModel::updateChdDiscType,
                        onConverterToolSelected = viewModel::updateConverterTool,
                        onDeleteOriginalChdFilesChanged = viewModel::updateDeleteOriginalChdFiles,
                        onScanHiddenFoldersChanged = viewModel::updateScanHiddenFolders,
                        onDarkModeToggled = viewModel::updateDarkMode,
                        onVitaQueryChanged = viewModel::updateVitaQuery,
                        onVitaShortcutFormatSelected = viewModel::updateVitaShortcutFormat,
                        onVitaShortcutAdd = viewModel::addVitaShortcut,
                        onVitaShortcutRemove = viewModel::removeVitaShortcut,
                        onChangeSelection = viewModel::updateChangeSelection,
                        onSelectAllChanges = viewModel::selectAllChanges,
                        onDeselectAllChanges = viewModel::deselectAllChanges,
                        onStart = viewModel::scan,
                        onApply = viewModel::applyChanges,
                        onStopNow = viewModel::stopNow,
                        onStopAfterCurrent = viewModel::stopAfterCurrentConversion,
                        onSelectPatchRom = { patchRomLauncher.launch(arrayOf("*/*")) },
                        onSelectPatchFile = { patchFileLauncher.launch(arrayOf("*/*")) },
                        onGrantPatchFolder = { patchFolderLauncher.launch(uiState.romPatch.rom?.uri) },
                        onApplyPatch = viewModel::applyPatch,
                    )
                }
            }
        }
    }
}
