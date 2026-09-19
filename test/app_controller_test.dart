import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mimir_core/mimir_core.dart';

import 'package:mimir_flutter/platform/platform_adapter.dart';
import 'package:mimir_flutter/state/app_state.dart';
import 'package:mimir_flutter/state/ui_message.dart';

void main() {
  late _FakePlatformAdapter platform;
  late ProviderContainer container;

  setUp(() async {
    platform = _FakePlatformAdapter();
    container = ProviderContainer(
      overrides: [
        platformAdapterProvider.overrideWithValue(platform),
        settingsStoreProvider.overrideWithValue(_MemorySettingsStore()),
      ],
    );
    container.read(appControllerProvider);
    await pumpEventQueue(times: 20);
  });

  tearDown(() => container.dispose());

  AppController controller() => container.read(appControllerProvider.notifier);
  AppState state() => container.read(appControllerProvider);

  test('selecting a tool chooses its section, mode, and converter', () {
    controller().selectSection(AppSection.rvz);

    expect(state().currentSection, AppSection.rvz);
    expect(state().selectedMode, ToolMode.chdConverter);
    expect(state().selectedConverterTool, ConverterTool.dolphinRvz);
  });

  test('scan requires a ROM folder before filesystem work starts', () async {
    await controller().scan();

    expect(state().message?.kind, UiMessageKind.selectRomFolderFirst);
    expect(platform.scanCalls, isZero);
  });

  test(
    'NSZ conversion requires prod.keys before filesystem work starts',
    () async {
      controller().selectTool(
        section: AppSection.nsz,
        mode: ToolMode.chdConverter,
        converter: ConverterTool.nszNsp,
      );
      await controller().selectRomFolder();
      await controller().scan();

      expect(state().message?.kind, UiMessageKind.importKeysFirst);
      expect(platform.scanCalls, isZero);
    },
  );

  test('zip scan previews selected output and applies that plan', () async {
    platform.entries = const [
      RomEntry(relativePath: 'Metroid.nes', fileName: 'Metroid.nes'),
    ];
    controller().selectSection(AppSection.zipper);
    await controller().selectRomFolder();

    await controller().scan();

    expect(state().busy, isFalse);
    expect(state().previewPlan?.changes.single.detailPath, 'Metroid.zip');
    expect(state().selectedChangePaths, {'Metroid.zip'});
    expect(platform.storageInfoCalls, isZero);

    await controller().applyChanges();

    expect(platform.appliedRoot, '/library');
    expect(platform.appliedPlan?.operations, hasLength(1));
    expect(state().busy, isFalse);
    expect(state().operationProgress, isNull);
    expect(state().message?.kind, UiMessageKind.changesApplied);
  });

  test(
    'scan failure clears busy state and preserves the failure detail',
    () async {
      platform.scanError = StateError('folder cannot be read');
      await controller().selectRomFolder();

      await controller().scan();

      expect(state().busy, isFalse);
      expect(state().message?.kind, UiMessageKind.raw);
      expect(state().message?.text, 'folder cannot be read');
    },
  );
}

class _MemorySettingsStore extends SettingsStore {
  final values = <String, Object?>{};

  @override
  Future<String?> getString(String key) async => values[key] as String?;

  @override
  Future<bool> getBool(String key, {required bool fallback}) async =>
      values[key] as bool? ?? fallback;

  @override
  Future<void> setString(String key, String value) async => values[key] = value;

  @override
  Future<void> setBool(String key, bool value) async => values[key] = value;
}

class _FakePlatformAdapter implements MimirPlatformAdapter {
  List<RomEntry> entries = const [];
  Object? scanError;
  int scanCalls = 0;
  int storageInfoCalls = 0;
  String? appliedRoot;
  OperationPlan? appliedPlan;

  @override
  Future<String?> pickDirectory({
    required PickerPurpose purpose,
    String? initialHandle,
  }) async => '/library';

  @override
  Future<String?> pickFile({required PickerPurpose purpose}) async => null;

  @override
  Future<List<RomEntry>> scan({
    required String rootHandle,
    required ScanRequest request,
    bool scanHiddenFolders = false,
    void Function(int scannedFiles)? onProgress,
    CancellationToken? cancellation,
  }) async {
    scanCalls++;
    if (scanError != null) throw scanError!;
    onProgress?.call(entries.length);
    return entries;
  }

  @override
  Future<StorageInfo?> storageInfo(String rootHandle) async {
    storageInfoCalls++;
    return const StorageInfo(totalBytes: 100, freeBytes: 50);
  }

  @override
  Stream<OperationEvent> apply({
    required String rootHandle,
    required OperationPlan plan,
    CancellationToken? cancellation,
  }) {
    appliedRoot = rootHandle;
    appliedPlan = plan;
    return Stream.fromIterable([
      OperationEvent(
        completed: plan.operations.length,
        total: plan.operations.length,
        finished: true,
      ),
    ]);
  }

  @override
  Future<bool> hasNszKeys() async => false;

  @override
  Future<String?> findScummVmExecutable() async => null;

  @override
  Future<List<ScummVmGame>> detectScummVmGames({
    required String rootHandle,
    required String executableHandle,
    void Function(int completed)? onProgress,
    CancellationToken? cancellation,
  }) async => const [];

  @override
  Future<void> deleteOutputFile({
    required String rootHandle,
    required String relativePath,
  }) async {}

  @override
  Future<void> importNszKeys({required String sourceHandle}) async {}

  @override
  Future<Map<String, String>> loadExistingVitaShortcuts({
    required String rootHandle,
    required VitaShortcutFormat format,
  }) async => const {};

  @override
  Future<List<EsDeSystem>> loadInstalledEsDe({
    required String rootHandle,
    String? romRootHandle,
  }) async => const [];

  @override
  Future<List<EsDeSystem>> fetchEsDeSystems({
    required String romRootHandle,
  }) async => const [];

  @override
  Future<int> installEsDeSystems({
    required String esdeRootHandle,
    required String romRootHandle,
    required List<EsDeSystem> systems,
    required bool useLatestCatalog,
  }) async => 0;

  @override
  Future<String?> defaultEsDeSystemFolder({
    required String romRootHandle,
    required EsDeSystem system,
  }) async => null;
}
