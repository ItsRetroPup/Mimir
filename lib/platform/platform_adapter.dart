import 'dart:async';

import 'package:mimir_core/mimir_core.dart';

enum PickerPurpose {
  romRoot,
  vitaOutput,
  esdeRoot,
  esdeSystemFolder,
  nszKeys,
  scummVmRoot,
  scummVmExecutable,
}

class ScanRequest {
  const ScanRequest({required this.mode, this.converterTool, this.chdSystem});

  final ToolMode mode;
  final ConverterTool? converterTool;
  final ChdSystem? chdSystem;
}

class StorageInfo {
  const StorageInfo({required this.totalBytes, required this.freeBytes});

  final int totalBytes;
  final int freeBytes;
}

enum StopRequest { none, afterCurrent, now }

class CancellationToken {
  StopRequest _request = StopRequest.none;

  StopRequest get request => _request;

  void stopNow() => _request = StopRequest.now;

  void stopAfterCurrent() {
    if (_request == StopRequest.none) _request = StopRequest.afterCurrent;
  }

  bool get shouldStopBeforeNext => _request != StopRequest.none;

  bool get shouldInterruptCurrent => _request == StopRequest.now;
}

class OperationEvent {
  const OperationEvent({
    required this.completed,
    required this.total,
    this.current,
    this.currentProgress,
    this.spaceSavedBytes = 0,
    this.stopped = false,
    this.finished = false,
    this.error,
  });

  final int completed;
  final int total;
  final String? current;
  final double? currentProgress;
  final int spaceSavedBytes;
  final bool stopped;
  final bool finished;
  final Object? error;

  Map<String, Object?> toWireJson() => operationEventToJson(
    OperationEventData(
      completed: completed,
      total: total,
      current: current,
      currentProgress: currentProgress,
      spaceSavedBytes: spaceSavedBytes,
      stopped: stopped,
      finished: finished,
      error: error?.toString(),
    ),
  );
}

class VitaExistingShortcut {
  const VitaExistingShortcut(this.titleId, this.relativePath);

  final String titleId;
  final String relativePath;
}

class EsDeSystem {
  const EsDeSystem({
    required this.name,
    required this.fullName,
    required this.defaultPath,
    required this.romFolder,
  });

  final String name;
  final String fullName;
  final String defaultPath;
  final String romFolder;

  bool get isDefault =>
      defaultPath.startsWith('%ROMPATH%/') &&
      romFolder == defaultPath.substring('%ROMPATH%/'.length);

  EsDeSystem copyWith({String? defaultPath, String? romFolder}) => EsDeSystem(
    name: name,
    fullName: fullName,
    defaultPath: defaultPath ?? this.defaultPath,
    romFolder: romFolder ?? this.romFolder,
  );
}

abstract interface class MimirPlatformAdapter {
  Future<String?> pickDirectory({
    required PickerPurpose purpose,
    String? initialHandle,
  });

  Future<String?> pickFile({required PickerPurpose purpose});

  Future<List<RomEntry>> scan({
    required String rootHandle,
    required ScanRequest request,
    bool scanHiddenFolders = false,
    void Function(int scannedFiles)? onProgress,
    CancellationToken? cancellation,
  });

  Future<String?> findScummVmExecutable();

  Future<List<ScummVmGame>> detectScummVmGames({
    required String rootHandle,
    required String executableHandle,
    void Function(int completed)? onProgress,
    CancellationToken? cancellation,
  });

  Future<StorageInfo?> storageInfo(String rootHandle);

  Stream<OperationEvent> apply({
    required String rootHandle,
    required OperationPlan plan,
    CancellationToken? cancellation,
  });

  Future<void> deleteOutputFile({
    required String rootHandle,
    required String relativePath,
  });

  Future<void> importNszKeys({required String sourceHandle});

  Future<bool> hasNszKeys();

  Future<Map<String, String>> loadExistingVitaShortcuts({
    required String rootHandle,
    required VitaShortcutFormat format,
  });

  Future<List<EsDeSystem>> loadInstalledEsDe({
    required String rootHandle,
    String? romRootHandle,
  });

  Future<List<EsDeSystem>> fetchEsDeSystems({required String romRootHandle});

  Future<int> installEsDeSystems({
    required String esdeRootHandle,
    required String romRootHandle,
    required List<EsDeSystem> systems,
    required bool useLatestCatalog,
  });

  Future<String?> defaultEsDeSystemFolder({
    required String romRootHandle,
    required EsDeSystem system,
  });
}
