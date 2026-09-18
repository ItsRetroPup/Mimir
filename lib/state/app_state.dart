import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mimir_core/mimir_core.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../platform/desktop_adapter.dart';
import '../platform/method_channel_adapter.dart';
import '../platform/platform_adapter.dart';
import 'ui_message.dart';

enum AppSection {
  home,
  esDeSystems,
  zipper,
  organizer,
  chdMan,
  rvz,
  zcci,
  nsz,
  vita,
}

enum ChdSortOption { nameAscending, nameDescending, sizeLargest, sizeSmallest }

class ToolDoesNotUseRomScanningException implements Exception {
  const ToolDoesNotUseRomScanningException();
}

class AppState {
  const AppState({
    this.currentSection = AppSection.home,
    this.romRootHandle,
    this.romRootLabel = '',
    this.vitaOutputHandle,
    this.vitaOutputLabel = '',
    this.esdeRootHandle,
    this.esdeRootLabel = '',
    this.vitaShortcutFormat = VitaShortcutFormat.psvita,
    this.vitaQuery = '',
    this.vitaDatabaseSize = 0,
    this.vitaSearchResults = const [],
    this.addedVitaShortcuts = const {},
    this.selectedMode = ToolMode.multiDiscOrganizer,
    this.selectedPreset = FrontendPreset.esDe,
    this.selectedChdSystem = ChdSystem.dreamcast,
    this.selectedChdDiscType = ChdDiscType.cd,
    this.selectedConverterTool = ConverterTool.chd,
    this.nszKeysConfigured = false,
    this.esdeSystems = const [],
    this.esdeLatestRequested = false,
    this.deleteOriginalChdFiles = false,
    this.scanHiddenFolders = false,
    this.useDarkMode = true,
    this.busy = false,
    this.scanProgressLabel,
    this.operationProgressLabel,
    this.operationProgress,
    this.currentJobProgress,
    this.currentJobLabel,
    this.previewPlan,
    this.selectedChangePaths = const {},
    this.storageInfo,
    this.stopRequest = StopRequest.none,
    this.completedOperations = 0,
    this.totalOperations = 0,
    this.spaceSavedBytes = 0,
    this.message,
  });

  final AppSection currentSection;
  final String? romRootHandle;
  final String romRootLabel;
  final String? vitaOutputHandle;
  final String vitaOutputLabel;
  final String? esdeRootHandle;
  final String esdeRootLabel;
  final VitaShortcutFormat vitaShortcutFormat;
  final String vitaQuery;
  final int vitaDatabaseSize;
  final List<VitaApp> vitaSearchResults;
  final Map<String, String> addedVitaShortcuts;
  final ToolMode selectedMode;
  final FrontendPreset selectedPreset;
  final ChdSystem selectedChdSystem;
  final ChdDiscType selectedChdDiscType;
  final ConverterTool selectedConverterTool;
  final bool nszKeysConfigured;
  final List<EsDeSystem> esdeSystems;
  final bool esdeLatestRequested;
  final bool deleteOriginalChdFiles;
  final bool scanHiddenFolders;
  final bool useDarkMode;
  final bool busy;
  final UiMessage? scanProgressLabel;
  final UiMessage? operationProgressLabel;
  final double? operationProgress;
  final double? currentJobProgress;
  final String? currentJobLabel;
  final OperationPlan? previewPlan;
  final Set<String> selectedChangePaths;
  final StorageInfo? storageInfo;
  final StopRequest stopRequest;
  final int completedOperations;
  final int totalOperations;
  final int spaceSavedBytes;
  final UiMessage? message;

  static const _unset = Object();

  AppState copyWith({
    AppSection? currentSection,
    Object? romRootHandle = _unset,
    String? romRootLabel,
    Object? vitaOutputHandle = _unset,
    String? vitaOutputLabel,
    Object? esdeRootHandle = _unset,
    String? esdeRootLabel,
    VitaShortcutFormat? vitaShortcutFormat,
    String? vitaQuery,
    int? vitaDatabaseSize,
    List<VitaApp>? vitaSearchResults,
    Map<String, String>? addedVitaShortcuts,
    ToolMode? selectedMode,
    FrontendPreset? selectedPreset,
    ChdSystem? selectedChdSystem,
    ChdDiscType? selectedChdDiscType,
    ConverterTool? selectedConverterTool,
    bool? nszKeysConfigured,
    List<EsDeSystem>? esdeSystems,
    bool? esdeLatestRequested,
    bool? deleteOriginalChdFiles,
    bool? scanHiddenFolders,
    bool? useDarkMode,
    bool? busy,
    Object? scanProgressLabel = _unset,
    Object? operationProgressLabel = _unset,
    Object? operationProgress = _unset,
    Object? currentJobProgress = _unset,
    Object? currentJobLabel = _unset,
    Object? previewPlan = _unset,
    Set<String>? selectedChangePaths,
    Object? storageInfo = _unset,
    StopRequest? stopRequest,
    int? completedOperations,
    int? totalOperations,
    int? spaceSavedBytes,
    Object? message = _unset,
  }) {
    return AppState(
      currentSection: currentSection ?? this.currentSection,
      romRootHandle: identical(romRootHandle, _unset)
          ? this.romRootHandle
          : romRootHandle as String?,
      romRootLabel: romRootLabel ?? this.romRootLabel,
      vitaOutputHandle: identical(vitaOutputHandle, _unset)
          ? this.vitaOutputHandle
          : vitaOutputHandle as String?,
      vitaOutputLabel: vitaOutputLabel ?? this.vitaOutputLabel,
      esdeRootHandle: identical(esdeRootHandle, _unset)
          ? this.esdeRootHandle
          : esdeRootHandle as String?,
      esdeRootLabel: esdeRootLabel ?? this.esdeRootLabel,
      vitaShortcutFormat: vitaShortcutFormat ?? this.vitaShortcutFormat,
      vitaQuery: vitaQuery ?? this.vitaQuery,
      vitaDatabaseSize: vitaDatabaseSize ?? this.vitaDatabaseSize,
      vitaSearchResults: vitaSearchResults ?? this.vitaSearchResults,
      addedVitaShortcuts: addedVitaShortcuts ?? this.addedVitaShortcuts,
      selectedMode: selectedMode ?? this.selectedMode,
      selectedPreset: selectedPreset ?? this.selectedPreset,
      selectedChdSystem: selectedChdSystem ?? this.selectedChdSystem,
      selectedChdDiscType: selectedChdDiscType ?? this.selectedChdDiscType,
      selectedConverterTool:
          selectedConverterTool ?? this.selectedConverterTool,
      nszKeysConfigured: nszKeysConfigured ?? this.nszKeysConfigured,
      esdeSystems: esdeSystems ?? this.esdeSystems,
      esdeLatestRequested: esdeLatestRequested ?? this.esdeLatestRequested,
      deleteOriginalChdFiles:
          deleteOriginalChdFiles ?? this.deleteOriginalChdFiles,
      scanHiddenFolders: scanHiddenFolders ?? this.scanHiddenFolders,
      useDarkMode: useDarkMode ?? this.useDarkMode,
      busy: busy ?? this.busy,
      scanProgressLabel: identical(scanProgressLabel, _unset)
          ? this.scanProgressLabel
          : scanProgressLabel as UiMessage?,
      operationProgressLabel: identical(operationProgressLabel, _unset)
          ? this.operationProgressLabel
          : operationProgressLabel as UiMessage?,
      operationProgress: identical(operationProgress, _unset)
          ? this.operationProgress
          : operationProgress as double?,
      currentJobProgress: identical(currentJobProgress, _unset)
          ? this.currentJobProgress
          : currentJobProgress as double?,
      currentJobLabel: identical(currentJobLabel, _unset)
          ? this.currentJobLabel
          : currentJobLabel as String?,
      previewPlan: identical(previewPlan, _unset)
          ? this.previewPlan
          : previewPlan as OperationPlan?,
      selectedChangePaths: selectedChangePaths ?? this.selectedChangePaths,
      storageInfo: identical(storageInfo, _unset)
          ? this.storageInfo
          : storageInfo as StorageInfo?,
      stopRequest: stopRequest ?? this.stopRequest,
      completedOperations: completedOperations ?? this.completedOperations,
      totalOperations: totalOperations ?? this.totalOperations,
      spaceSavedBytes: spaceSavedBytes ?? this.spaceSavedBytes,
      message: identical(message, _unset)
          ? this.message
          : message as UiMessage?,
    );
  }
}

final settingsStoreProvider = Provider<SettingsStore>((ref) => SettingsStore());

final platformAdapterProvider = Provider<MimirPlatformAdapter>((ref) {
  if (Platform.isAndroid) return MethodChannelAdapter();
  return DesktopPlatformAdapter();
});

final appControllerProvider = NotifierProvider<AppController, AppState>(
  AppController.new,
);

class SettingsStore {
  static const romRoot = 'rom_tree_uri';
  static const romRootLabel = 'rom_tree_label';
  static const vitaOutput = 'vita_output_uri';
  static const vitaOutputLabel = 'vita_output_label';
  static const esdeRoot = 'esde_root_uri';
  static const esdeRootLabel = 'esde_root_label';
  static const vitaFormat = 'vita_shortcut_format';
  static const darkMode = 'dark_mode';
  static const scanHidden = 'scan_hidden_folders';
  static const deleteChd = 'delete_original_chd_files';
  static const _legacyChannel = MethodChannel('pup.app.mimir/platform');

  Future<SharedPreferences> get _preferences => SharedPreferences.getInstance();

  Future<String?> getString(String key) async {
    final preferences = await _preferences;
    final local = preferences.getString(key);
    if (local != null || !Platform.isAndroid) return local;
    final legacy = await _legacyChannel.invokeMethod<Object?>('legacySetting', {
      'key': key,
    });
    if (legacy is! String) return null;
    await preferences.setString(key, legacy);
    return legacy;
  }

  Future<bool> getBool(String key, {required bool fallback}) async {
    final preferences = await _preferences;
    final local = preferences.getBool(key);
    if (local != null || !Platform.isAndroid) return local ?? fallback;
    final legacy = await _legacyChannel.invokeMethod<Object?>('legacySetting', {
      'key': key,
    });
    if (legacy is bool) {
      await preferences.setBool(key, legacy);
      return legacy;
    }
    return fallback;
  }

  Future<void> setString(String key, String value) async {
    await (await _preferences).setString(key, value);
    if (Platform.isAndroid) {
      await _legacyChannel.invokeMethod<void>('saveLegacySetting', {
        'key': key,
        'value': value,
      });
    }
  }

  Future<void> setBool(String key, bool value) async {
    await (await _preferences).setBool(key, value);
    if (Platform.isAndroid) {
      await _legacyChannel.invokeMethod<void>('saveLegacySetting', {
        'key': key,
        'value': value,
      });
    }
  }
}

class AppController extends Notifier<AppState> {
  late final MimirPlatformAdapter _platform;
  late final SettingsStore _settings;
  CancellationToken? _activeCancellation;
  List<VitaApp> _vitaCatalog = const [];

  @override
  AppState build() {
    _platform = ref.read(platformAdapterProvider);
    _settings = ref.read(settingsStoreProvider);
    unawaited(_loadPersistedState());
    return const AppState();
  }

  Future<void> _loadPersistedState() async {
    final values = await Future.wait([
      _settings.getString(SettingsStore.romRoot),
      _settings.getString(SettingsStore.romRootLabel),
      _settings.getString(SettingsStore.vitaOutput),
      _settings.getString(SettingsStore.vitaOutputLabel),
      _settings.getString(SettingsStore.esdeRoot),
      _settings.getString(SettingsStore.esdeRootLabel),
      _settings.getString(SettingsStore.vitaFormat),
      _settings.getBool(SettingsStore.darkMode, fallback: true),
      _settings.getBool(SettingsStore.scanHidden, fallback: false),
      _settings.getBool(SettingsStore.deleteChd, fallback: false),
    ]);
    final catalog = await _loadVitaCatalog();
    _vitaCatalog = catalog;
    final nszKeysConfigured = await _platform.hasNszKeys();
    final format =
        VitaShortcutFormat.values
            .where((value) => value.name == values[6])
            .firstOrNull ??
        VitaShortcutFormat.psvita;
    state = state.copyWith(
      romRootHandle: values[0] as String?,
      romRootLabel: values[1] as String? ?? '',
      vitaOutputHandle: values[2] as String?,
      vitaOutputLabel: values[3] as String? ?? '',
      esdeRootHandle: values[4] as String?,
      esdeRootLabel: values[5] as String? ?? '',
      vitaShortcutFormat: format,
      useDarkMode: values[7] as bool,
      scanHiddenFolders: values[8] as bool,
      deleteOriginalChdFiles: values[9] as bool,
      nszKeysConfigured: nszKeysConfigured,
      vitaDatabaseSize: catalog.length,
    );
    final output = state.vitaOutputHandle;
    if (output != null) await _refreshExistingVitaShortcuts(output);
    final esde = state.esdeRootHandle;
    if (esde != null) {
      try {
        final systems = await _platform.loadInstalledEsDe(
          rootHandle: esde,
          romRootHandle: state.romRootHandle,
        );
        state = state.copyWith(esdeSystems: systems);
      } catch (_) {
        // A stale folder handle is retained so the user can replace it from the UI.
      }
    }
  }

  Future<List<VitaApp>> _loadVitaCatalog() async {
    try {
      final contents = await rootBundle.loadString(
        'assets/data/vita_shortcuts.tsv',
      );
      return contents
          .split(RegExp(r'\r?\n'))
          .map((line) {
            final parts = line.split('\t');
            if (parts.length != 2) return null;
            final title = parts[0].trim();
            final appId = parts[1].trim();
            if (title.isEmpty || appId.isEmpty) return null;
            return VitaApp(
              titleId: appId,
              title: title,
              sourcePath: 'shortcut-db',
            );
          })
          .whereType<VitaApp>()
          .toList();
    } catch (_) {
      return const [];
    }
  }

  void selectSection(AppSection section) {
    final defaults = switch (section) {
      AppSection.home => (
        mode: state.selectedMode,
        converter: state.selectedConverterTool,
      ),
      AppSection.organizer => (
        mode: ToolMode.multiDiscOrganizer,
        converter: state.selectedConverterTool,
      ),
      AppSection.zipper => (
        mode: ToolMode.romZipper,
        converter: state.selectedConverterTool,
      ),
      AppSection.chdMan => (
        mode: ToolMode.chdConverter,
        converter: ConverterTool.chd,
      ),
      AppSection.rvz => (
        mode: ToolMode.chdConverter,
        converter: ConverterTool.dolphinRvz,
      ),
      AppSection.zcci => (
        mode: ToolMode.chdConverter,
        converter: ConverterTool.azaharZcci,
      ),
      AppSection.nsz => (
        mode: ToolMode.chdConverter,
        converter: ConverterTool.nszNsp,
      ),
      AppSection.vita => (
        mode: ToolMode.vitaAppIds,
        converter: state.selectedConverterTool,
      ),
      AppSection.esDeSystems => (
        mode: ToolMode.esDeSystems,
        converter: state.selectedConverterTool,
      ),
    };
    state = state.copyWith(
      currentSection: section,
      selectedMode: defaults.mode,
      selectedConverterTool: defaults.converter,
    );
  }

  void selectTool({
    required AppSection section,
    required ToolMode mode,
    ConverterTool? converter,
  }) {
    state = state.copyWith(
      currentSection: section,
      selectedMode: mode,
      selectedConverterTool: converter ?? state.selectedConverterTool,
      previewPlan: null,
      selectedChangePaths: {},
      message: null,
    );
  }

  Future<void> selectRomFolder() async {
    final handle = await _platform.pickDirectory(
      purpose: PickerPurpose.romRoot,
      initialHandle: state.romRootHandle,
    );
    if (handle == null) return;
    await _settings.setString(SettingsStore.romRoot, handle);
    await _settings.setString(
      SettingsStore.romRootLabel,
      _labelForHandle(handle),
    );
    state = state.copyWith(
      romRootHandle: handle,
      romRootLabel: _labelForHandle(handle),
      previewPlan: null,
      selectedChangePaths: {},
      storageInfo: null,
      message: null,
    );
  }

  Future<void> selectVitaOutput() async {
    final handle = await _platform.pickDirectory(
      purpose: PickerPurpose.vitaOutput,
      initialHandle: state.vitaOutputHandle,
    );
    if (handle == null) return;
    final label = _labelForHandle(handle);
    await _settings.setString(SettingsStore.vitaOutput, handle);
    await _settings.setString(SettingsStore.vitaOutputLabel, label);
    state = state.copyWith(
      vitaOutputHandle: handle,
      vitaOutputLabel: label,
      addedVitaShortcuts: {},
      message: null,
    );
    await _refreshExistingVitaShortcuts(handle);
  }

  Future<void> selectEsdeRoot() async {
    final handle = await _platform.pickDirectory(
      purpose: PickerPurpose.esdeRoot,
      initialHandle: state.esdeRootHandle,
    );
    if (handle == null) return;
    final label = _labelForHandle(handle);
    await _settings.setString(SettingsStore.esdeRoot, handle);
    await _settings.setString(SettingsStore.esdeRootLabel, label);
    state = state.copyWith(
      esdeRootHandle: handle,
      esdeRootLabel: label,
      esdeSystems: [],
      esdeLatestRequested: false,
      message: null,
    );
    try {
      state = state.copyWith(
        esdeSystems: await _platform.loadInstalledEsDe(
          rootHandle: handle,
          romRootHandle: state.romRootHandle,
        ),
      );
    } catch (error) {
      state = state.copyWith(message: UiMessage.raw(error.toString()));
    }
  }

  Future<void> selectNszKeys() async {
    final handle = await _platform.pickFile(purpose: PickerPurpose.nszKeys);
    if (handle == null) return;
    state = state.copyWith(busy: true, message: null);
    try {
      await _platform.importNszKeys(sourceHandle: handle);
      state = state.copyWith(
        busy: false,
        nszKeysConfigured: true,
        message: const UiMessage.prodKeysImported(),
      );
    } catch (error) {
      state = state.copyWith(
        busy: false,
        message: _errorMessage(error, const UiMessage.importKeysFailed()),
      );
    }
  }

  void updatePreset(FrontendPreset preset) => state = state.copyWith(
    selectedPreset: preset,
    previewPlan: null,
    selectedChangePaths: {},
    message: null,
  );

  void updateChdSystem(ChdSystem system) {
    state = state.copyWith(
      selectedChdSystem: system,
      selectedChdDiscType: system == ChdSystem.playStationPortable
          ? ChdDiscType.dvd
          : system == ChdSystem.playStation2
          ? state.selectedChdDiscType
          : ChdDiscType.cd,
      previewPlan: null,
      selectedChangePaths: {},
      message: null,
    );
  }

  void updateChdDiscType(ChdDiscType discType) {
    if (state.selectedChdSystem != ChdSystem.playStation2) return;
    state = state.copyWith(
      selectedChdDiscType: discType,
      previewPlan: null,
      selectedChangePaths: {},
      message: null,
    );
  }

  void updateConverterTool(ConverterTool tool) => state = state.copyWith(
    selectedConverterTool: tool,
    previewPlan: null,
    selectedChangePaths: {},
    message: null,
  );

  Future<void> updateDeleteOriginalChdFiles(bool enabled) async {
    await _settings.setBool(SettingsStore.deleteChd, enabled);
    state = state.copyWith(deleteOriginalChdFiles: enabled, message: null);
  }

  Future<void> updateScanHiddenFolders(bool enabled) async {
    await _settings.setBool(SettingsStore.scanHidden, enabled);
    state = state.copyWith(
      scanHiddenFolders: enabled,
      previewPlan: null,
      selectedChangePaths: {},
      message: null,
    );
  }

  Future<void> updateDarkMode(bool enabled) async {
    await _settings.setBool(SettingsStore.darkMode, enabled);
    state = state.copyWith(useDarkMode: enabled);
  }

  void updateVitaQuery(String query) {
    final normalized = query.trim().toLowerCase();
    final results = normalized.isEmpty
        ? const <VitaApp>[]
        : _vitaCatalog
              .where(
                (app) =>
                    app.title.toLowerCase().contains(normalized) ||
                    app.titleId.toLowerCase().contains(normalized),
              )
              .take(40)
              .toList();
    state = state.copyWith(
      vitaQuery: query,
      vitaSearchResults: results,
      message: null,
    );
  }

  Future<void> updateVitaShortcutFormat(VitaShortcutFormat format) async {
    await _settings.setString(SettingsStore.vitaFormat, format.name);
    state = state.copyWith(
      vitaShortcutFormat: format,
      addedVitaShortcuts: {},
      message: null,
    );
    final output = state.vitaOutputHandle;
    if (output != null) await _refreshExistingVitaShortcuts(output);
  }

  void updateChangeSelection(String path, bool selected) {
    if (state.previewPlan?.changes.any((change) => change.detailPath == path) !=
        true) {
      return;
    }
    final selection = {...state.selectedChangePaths};
    if (selected) {
      selection.add(path);
    } else {
      selection.remove(path);
    }
    state = state.copyWith(selectedChangePaths: selection);
  }

  void selectAllChanges() {
    final plan = state.previewPlan;
    if (plan == null) return;
    final selected = plan.changes
        .where(
          (change) =>
              !(state.selectedMode == ToolMode.chdConverter &&
                  change.targetAlreadyExists),
        )
        .map((change) => change.detailPath)
        .toSet();
    state = state.copyWith(selectedChangePaths: selected);
  }

  void deselectAllChanges() => state = state.copyWith(selectedChangePaths: {});

  Future<void> scan() async {
    final current = state;
    final root = current.romRootHandle;
    if (root == null) {
      state = state.copyWith(message: const UiMessage.selectRomFolderFirst());
      return;
    }
    if (current.selectedMode == ToolMode.chdConverter &&
        current.selectedConverterTool == ConverterTool.nszNsp &&
        !current.nszKeysConfigured) {
      state = state.copyWith(message: const UiMessage.importKeysFirst());
      return;
    }
    final cancellation = CancellationToken();
    _activeCancellation = cancellation;
    state = state.copyWith(
      busy: true,
      scanProgressLabel: _initialScanLabel(current),
      message: null,
      stopRequest: StopRequest.none,
    );
    try {
      final entries = await _platform.scan(
        rootHandle: root,
        request: ScanRequest(
          mode: current.selectedMode,
          converterTool: current.selectedConverterTool,
          chdSystem: current.selectedChdSystem,
        ),
        scanHiddenFolders: current.scanHiddenFolders,
        cancellation: cancellation,
        onProgress: (count) => state = state.copyWith(
          scanProgressLabel: UiMessage.scanFileCount(
            current.selectedMode,
            current.selectedConverterTool,
            count,
          ),
        ),
      );
      if (cancellation.shouldInterruptCurrent) {
        throw const OperationStoppedException('Scan stopped.');
      }
      final plan = switch (current.selectedMode) {
        ToolMode.multiDiscOrganizer => ChangePlanner.buildPlan(
          scanResult: RomScanner.scan(entries),
          preset: current.selectedPreset,
        ),
        ToolMode.romZipper => RomZipperPlanner.buildPlan(entries),
        ToolMode.chdConverter =>
          current.selectedConverterTool == ConverterTool.chd
              ? ChdPlanner.buildPlan(
                  entries: entries,
                  system: current.selectedChdSystem,
                  discType: current.selectedChdDiscType,
                  deleteOriginalFiles: current.deleteOriginalChdFiles,
                )
              : NativeConverterPlanner.buildPlan(
                  entries: entries,
                  tool: current.selectedConverterTool,
                ),
        ToolMode.vitaAppIds || ToolMode.esDeSystems =>
          throw const ToolDoesNotUseRomScanningException(),
      };
      final selected = plan.changes
          .where(
            (change) =>
                !(current.selectedMode == ToolMode.chdConverter &&
                    change.targetAlreadyExists),
          )
          .map((change) => change.detailPath)
          .toSet();
      state = state.copyWith(
        busy: false,
        scanProgressLabel: null,
        previewPlan: plan,
        selectedChangePaths: selected,
        storageInfo: current.selectedMode == ToolMode.chdConverter
            ? await _platform.storageInfo(root)
            : current.storageInfo,
        message: plan.changes.isEmpty ? _noChangesMessage(current) : null,
        stopRequest: StopRequest.none,
      );
    } catch (error) {
      state = state.copyWith(
        busy: false,
        scanProgressLabel: null,
        stopRequest: StopRequest.none,
        message: _errorMessage(error, const UiMessage.scanFailed()),
      );
    } finally {
      if (identical(_activeCancellation, cancellation)) {
        _activeCancellation = null;
      }
    }
  }

  Future<void> applyChanges() async {
    final current = state;
    final root = current.romRootHandle;
    final originalPlan = current.previewPlan;
    if (root == null || originalPlan == null) return;
    var plan = originalPlan.forSelectedChanges(current.selectedChangePaths);
    if (plan.mode == ToolMode.chdConverter &&
        current.selectedConverterTool == ConverterTool.chd) {
      plan = _withChdDeleteOriginalFiles(plan, current.deleteOriginalChdFiles);
    }
    if (plan.operations.isEmpty) return;
    final cancellation = CancellationToken();
    _activeCancellation = cancellation;
    state = state.copyWith(
      busy: true,
      operationProgressLabel: UiMessage.applying(
        plan.mode,
        current.selectedConverterTool,
        0,
        plan.operations.length,
      ),
      operationProgress: 0,
      currentJobProgress: plan.mode == ToolMode.chdConverter ? 0 : null,
      currentJobLabel: null,
      completedOperations: 0,
      totalOperations: plan.operations.length,
      spaceSavedBytes: 0,
      message: null,
      stopRequest: StopRequest.none,
    );
    try {
      await for (final event in _platform.apply(
        rootHandle: root,
        plan: plan,
        cancellation: cancellation,
      )) {
        state = state.copyWith(
          operationProgressLabel: UiMessage.applying(
            plan.mode,
            current.selectedConverterTool,
            event.completed,
            event.total,
          ),
          operationProgress: event.total == 0
              ? 0
              : event.completed / event.total,
          currentJobProgress: event.currentProgress,
          currentJobLabel: event.current,
          completedOperations: event.completed,
          totalOperations: event.total,
          spaceSavedBytes: event.spaceSavedBytes,
          stopRequest: event.stopped
              ? (cancellation.request == StopRequest.now
                    ? StopRequest.now
                    : StopRequest.afterCurrent)
              : state.stopRequest,
        );
        if (event.finished) {
          state = state.copyWith(
            busy: false,
            operationProgressLabel: null,
            operationProgress: null,
            currentJobProgress: null,
            currentJobLabel: null,
            stopRequest: StopRequest.none,
            message: event.stopped
                ? UiMessage.stoppedAfterOperations(event.completed, event.total)
                : const UiMessage.changesApplied(),
          );
        }
      }
      if (state.busy) {
        state = state.copyWith(
          busy: false,
          operationProgressLabel: null,
          operationProgress: null,
          currentJobProgress: null,
          currentJobLabel: null,
          stopRequest: StopRequest.none,
          message: const UiMessage.changesApplied(),
        );
      }
    } catch (error) {
      state = state.copyWith(
        busy: false,
        operationProgressLabel: null,
        operationProgress: null,
        currentJobProgress: null,
        currentJobLabel: null,
        stopRequest: StopRequest.none,
        message: _errorMessage(error, const UiMessage.applyFailed()),
      );
    } finally {
      if (identical(_activeCancellation, cancellation)) {
        _activeCancellation = null;
      }
    }
  }

  void stopNow() {
    _activeCancellation?.stopNow();
    state = state.copyWith(
      stopRequest: StopRequest.now,
      message: const UiMessage.stopping(),
    );
  }

  void stopAfterCurrent() {
    _activeCancellation?.stopAfterCurrent();
    state = state.copyWith(
      stopRequest: StopRequest.afterCurrent,
      message: const UiMessage.stopAfterCurrent(),
    );
  }

  Future<void> addVitaShortcut(VitaApp app) async {
    final output = state.vitaOutputHandle;
    if (output == null) {
      state = state.copyWith(message: const UiMessage.selectVitaOutputFirst());
      return;
    }
    state = state.copyWith(busy: true, message: null);
    try {
      final existing = await _platform.loadExistingVitaShortcuts(
        rootHandle: output,
        format: state.vitaShortcutFormat,
      );
      final plan = VitaAppIdPlanner.buildPlan(
        apps: [app],
        existingEntries: existing.entries
            .map(
              (item) => RomEntry(
                relativePath: item.value,
                fileName: item.value.split('/').last,
              ),
            )
            .toList(),
        format: state.vitaShortcutFormat,
      );
      if (plan.changes.isEmpty) {
        state = state.copyWith(
          busy: false,
          message:
              plan.conflicts.firstOrNull ??
              UiMessage.unableToAddShortcut(app.title),
        );
        return;
      }
      await _consumeApply(output, plan);
      state = state.copyWith(
        busy: false,
        addedVitaShortcuts: {
          ...state.addedVitaShortcuts,
          app.titleId: plan.changes.single.targetFiles.single,
        },
        message: UiMessage.addedShortcut(app.title),
      );
    } catch (error) {
      state = state.copyWith(
        busy: false,
        message: _errorMessage(error, const UiMessage.addShortcutFailed()),
      );
    }
  }

  Future<void> removeVitaShortcut(VitaApp app) async {
    final output = state.vitaOutputHandle;
    final path = state.addedVitaShortcuts[app.titleId];
    if (output == null || path == null) return;
    state = state.copyWith(busy: true, message: null);
    try {
      await _platform.deleteOutputFile(rootHandle: output, relativePath: path);
      state = state.copyWith(
        busy: false,
        addedVitaShortcuts: {...state.addedVitaShortcuts}..remove(app.titleId),
        message: UiMessage.removedShortcut(app.title),
      );
    } catch (error) {
      state = state.copyWith(
        busy: false,
        message: _errorMessage(error, const UiMessage.removeShortcutFailed()),
      );
    }
  }

  Future<void> refreshEsDeSystems() async {
    final romRoot = state.romRootHandle;
    final esdeRoot = state.esdeRootHandle;
    if (esdeRoot == null) {
      state = state.copyWith(message: const UiMessage.selectEsdeFolderFirst());
      return;
    }
    if (romRoot == null) {
      state = state.copyWith(message: const UiMessage.selectRomRootForEsde());
      return;
    }
    state = state.copyWith(
      busy: true,
      message: const UiMessage.downloadingEsde(),
    );
    try {
      final systems = await _platform.fetchEsDeSystems(romRootHandle: romRoot);
      state = state.copyWith(
        busy: false,
        esdeSystems: systems,
        esdeLatestRequested: true,
        message: UiMessage.downloadedEsde(systems.length),
      );
    } catch (error) {
      state = state.copyWith(
        busy: false,
        message: _errorMessage(error, const UiMessage.downloadEsdeFailed()),
      );
    }
  }

  Future<void> pickEsDeSystemFolder(String name) async {
    final system = state.esdeSystems
        .where((item) => item.name == name)
        .firstOrNull;
    final romRoot = state.romRootHandle;
    if (system == null || romRoot == null) return;
    final initial = await _platform.defaultEsDeSystemFolder(
      romRootHandle: romRoot,
      system: system,
    );
    final handle = await _platform.pickDirectory(
      purpose: PickerPurpose.esdeSystemFolder,
      initialHandle: initial,
    );
    if (handle == null) return;
    state = state.copyWith(
      esdeSystems: state.esdeSystems
          .map(
            (item) => item.name == name
                ? item.copyWith(defaultPath: handle, romFolder: handle)
                : item,
          )
          .toList(),
    );
  }

  Future<void> applyEsDeSystems() async {
    final esdeRoot = state.esdeRootHandle;
    final romRoot = state.romRootHandle;
    if (esdeRoot == null || romRoot == null || state.esdeSystems.isEmpty) {
      return;
    }
    state = state.copyWith(
      busy: true,
      message: const UiMessage.installingEsde(),
    );
    try {
      final count = await _platform.installEsDeSystems(
        esdeRootHandle: esdeRoot,
        romRootHandle: romRoot,
        systems: state.esdeSystems,
        useLatestCatalog: state.esdeLatestRequested,
      );
      state = state.copyWith(
        busy: false,
        esdeLatestRequested: false,
        message: UiMessage.installedEsde(count),
      );
    } catch (error) {
      state = state.copyWith(
        busy: false,
        message: _errorMessage(error, const UiMessage.installEsdeFailed()),
      );
    }
  }

  Future<void> _consumeApply(String root, OperationPlan plan) async {
    await for (final event in _platform.apply(rootHandle: root, plan: plan)) {
      if (event.error != null) throw event.error!;
    }
  }

  Future<void> _refreshExistingVitaShortcuts(String output) async {
    try {
      final existing = await _platform.loadExistingVitaShortcuts(
        rootHandle: output,
        format: state.vitaShortcutFormat,
      );
      state = state.copyWith(addedVitaShortcuts: existing);
    } catch (_) {
      state = state.copyWith(addedVitaShortcuts: {});
    }
  }

  OperationPlan _withChdDeleteOriginalFiles(OperationPlan plan, bool enabled) {
    FileOperation update(FileOperation operation) => operation is ConvertToChd
        ? ConvertToChd(
            sourcePath: operation.sourcePath,
            targetPath: operation.targetPath,
            system: operation.system,
            discType: operation.discType,
            deleteOriginalFiles: enabled,
          )
        : operation;
    return OperationPlan(
      mode: plan.mode,
      preset: plan.preset,
      changes: plan.changes
          .map(
            (change) => PlannedChange(
              title: change.title,
              sourceFiles: change.sourceFiles,
              targetFiles: change.targetFiles,
              detailLabel: change.detailLabel,
              detailPath: change.detailPath,
              operations: change.operations.map(update).toList(),
              sourceSizeBytes: change.sourceSizeBytes,
              targetAlreadyExists: change.targetAlreadyExists,
            ),
          )
          .toList(),
      operations: plan.operations.map(update).toList(),
      conflicts: plan.conflicts,
    );
  }

  String _labelForHandle(String handle) {
    final normalized = handle
        .replaceAll('\\', '/')
        .replaceAll(RegExp(r'/+$'), '');
    return normalized.substring(normalized.lastIndexOf('/') + 1).isEmpty
        ? normalized
        : normalized.substring(normalized.lastIndexOf('/') + 1);
  }

  UiMessage _initialScanLabel(AppState current) => UiMessage.scanStarted(
    current.selectedMode,
    converter: current.selectedConverterTool,
  );

  UiMessage _noChangesMessage(AppState current) =>
      switch (current.selectedMode) {
        ToolMode.multiDiscOrganizer => const UiMessage.noMultiDiscGames(),
        ToolMode.romZipper => const UiMessage.noZipCompatibleGames(),
        ToolMode.chdConverter => UiMessage.noCompatibleConverterFiles(
          current.selectedConverterTool,
        ),
        ToolMode.vitaAppIds => const UiMessage.noVitaShortcutsQueued(),
        ToolMode.esDeSystems => const UiMessage.noEsdeSystemsLoaded(),
      };

  UiMessage _errorMessage(Object error, UiMessage fallback) {
    if (error is OperationStoppedException) {
      return const UiMessage.operationStopped();
    }
    if (error is ToolDoesNotUseRomScanningException) {
      return const UiMessage.toolDoesNotUseRomScanning();
    }
    final message = error
        .toString()
        .replaceFirst('Bad state: ', '')
        .replaceFirst('Exception: ', '')
        .trim();
    if (message == 'Scan stopped.') return const UiMessage.scanStopped();
    return message.isEmpty ? fallback : UiMessage.raw(message);
  }
}
