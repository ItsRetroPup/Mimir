import 'dart:async';

import 'package:flutter/services.dart';
import 'package:mimir_core/mimir_core.dart';

import 'platform_adapter.dart';

/// Android's implementation of the platform seam.
///
/// The channel deliberately transports only opaque handles, relative paths,
/// and versioned plan/event maps. Android URI and SAF details stay on the
/// other side of this adapter.
class MethodChannelAdapter implements MimirPlatformAdapter {
  MethodChannelAdapter({MethodChannel? channel})
    : _channel = channel ?? const MethodChannel(_channelName);

  static const _channelName = 'pup.app.mimir/platform';
  static const _eventsChannelName = 'pup.app.mimir/platform/events';
  final MethodChannel _channel;

  @override
  Future<String?> pickDirectory({
    required PickerPurpose purpose,
    String? initialHandle,
  }) async {
    final result = await _channel.invokeMethod<Object?>('pickDirectory', {
      'purpose': purpose.name,
      'initialHandle': initialHandle,
    });
    return result as String?;
  }

  @override
  Future<String?> pickFile({required PickerPurpose purpose}) async {
    final result = await _channel.invokeMethod<Object?>('pickFile', {
      'purpose': purpose.name,
    });
    return result as String?;
  }

  @override
  Future<List<RomEntry>> scan({
    required String rootHandle,
    required ScanRequest request,
    bool scanHiddenFolders = false,
    void Function(int scannedFiles)? onProgress,
    CancellationToken? cancellation,
  }) async {
    Timer? stopTimer;
    var sentStop = StopRequest.none;
    try {
      stopTimer = Timer.periodic(const Duration(milliseconds: 100), (_) {
        final requested = cancellation?.request ?? StopRequest.none;
        if (requested != StopRequest.none && requested != sentStop) {
          sentStop = requested;
          unawaited(
            _channel.invokeMethod<void>('stopOperation', {
              'mode': requested.name,
            }),
          );
        }
      });
      final result = await _channel.invokeMethod<Object?>('scan', {
        'rootHandle': rootHandle,
        'mode': request.mode.name,
        'converterTool': request.converterTool?.name,
        'chdSystem': request.chdSystem?.name,
        'scanHiddenFolders': scanHiddenFolders,
      });
      if (cancellation?.shouldInterruptCurrent == true) {
        throw StateError('Scan stopped.');
      }
      if (result is! List) return const [];
      final entries = <RomEntry>[];
      for (final item in result) {
        if (item is! Map) continue;
        entries.add(_romEntry(item));
        onProgress?.call(entries.length);
      }
      return entries;
    } finally {
      stopTimer?.cancel();
    }
  }

  @override
  Future<StorageInfo?> storageInfo(String rootHandle) async {
    final raw = await _channel.invokeMethod<Object?>('storageInfo', {
      'rootHandle': rootHandle,
    });
    if (raw is! Map) return null;
    return StorageInfo(
      totalBytes: _asInt(raw['totalBytes']),
      freeBytes: _asInt(raw['freeBytes']),
    );
  }

  @override
  Stream<OperationEvent> apply({
    required String rootHandle,
    required OperationPlan plan,
    CancellationToken? cancellation,
  }) {
    final controller = StreamController<OperationEvent>();
    final token = cancellation ?? CancellationToken();
    unawaited(_apply(rootHandle, plan, token, controller));
    return controller.stream;
  }

  Future<void> _apply(
    String rootHandle,
    OperationPlan plan,
    CancellationToken cancellation,
    StreamController<OperationEvent> controller,
  ) async {
    Timer? stopTimer;
    var sentStop = StopRequest.none;
    StreamSubscription<dynamic>? events;
    final sessionId = DateTime.now().microsecondsSinceEpoch.toString();
    final done = Completer<void>();
    try {
      events = const EventChannel(_eventsChannelName)
          .receiveBroadcastStream(sessionId)
          .listen(
            (raw) {
              if (raw is! Map) return;
              final event = _operationEvent(raw, plan.operations.length);
              controller.add(event);
              if (event.finished && !done.isCompleted) done.complete();
            },
            onError: (Object error, StackTrace stackTrace) {
              if (!done.isCompleted) done.completeError(error, stackTrace);
            },
          );
      stopTimer = Timer.periodic(const Duration(milliseconds: 100), (_) {
        final request = cancellation.request;
        if (request != StopRequest.none && request != sentStop) {
          sentStop = request;
          unawaited(
            _channel.invokeMethod<void>('stopOperation', {
              'mode': request.name,
              'sessionId': sessionId,
            }),
          );
        }
      });
      final response = await _channel.invokeMethod<Object?>('apply', {
        'rootHandle': rootHandle,
        'plan': planToJson(plan),
        'sessionId': sessionId,
      });
      if (response is Map && response['finished'] == true) {
        final event = _operationEvent(response, plan.operations.length);
        controller.add(event);
        if (!done.isCompleted) done.complete();
      }
      await done.future;
      await controller.close();
    } on PlatformException catch (error, stackTrace) {
      controller.addError(StateError(error.message ?? error.code), stackTrace);
      await controller.close();
    } catch (error, stackTrace) {
      controller.addError(error, stackTrace);
      await controller.close();
    } finally {
      stopTimer?.cancel();
      await events?.cancel();
    }
  }

  @override
  Future<void> importNszKeys({required String sourceHandle}) async {
    await _channel.invokeMethod<void>('importNszKeys', {
      'sourceHandle': sourceHandle,
    });
  }

  @override
  Future<bool> hasNszKeys() async {
    final value = await _channel.invokeMethod<Object?>('hasNszKeys');
    return value == true;
  }

  @override
  Future<void> deleteOutputFile({
    required String rootHandle,
    required String relativePath,
  }) async {
    await _channel.invokeMethod<void>('deleteOutputFile', {
      'rootHandle': rootHandle,
      'relativePath': relativePath,
    });
  }

  @override
  Future<Map<String, String>> loadExistingVitaShortcuts({
    required String rootHandle,
    required VitaShortcutFormat format,
  }) async {
    final raw = await _channel.invokeMethod<Object?>(
      'loadExistingVitaShortcuts',
      {'rootHandle': rootHandle, 'format': format.name},
    );
    if (raw is! Map) return {};
    return raw.map((key, value) => MapEntry(key.toString(), value.toString()));
  }

  @override
  Future<List<EsDeSystem>> loadInstalledEsDe({
    required String rootHandle,
    String? romRootHandle,
  }) async {
    final raw = await _channel.invokeMethod<Object?>('loadInstalledEsDe', {
      'rootHandle': rootHandle,
      'romRootHandle': romRootHandle,
    });
    return _systems(raw);
  }

  @override
  Future<List<EsDeSystem>> fetchEsDeSystems({
    required String romRootHandle,
  }) async {
    final raw = await _channel.invokeMethod<Object?>('fetchEsDeSystems', {
      'romRootHandle': romRootHandle,
    });
    return _systems(raw);
  }

  @override
  Future<int> installEsDeSystems({
    required String esdeRootHandle,
    required String romRootHandle,
    required List<EsDeSystem> systems,
    required bool useLatestCatalog,
  }) async {
    final raw = await _channel.invokeMethod<Object?>('installEsDeSystems', {
      'esdeRootHandle': esdeRootHandle,
      'romRootHandle': romRootHandle,
      'systems': systems.map(_systemToJson).toList(),
      'useLatestCatalog': useLatestCatalog,
    });
    return _asInt(raw);
  }

  @override
  Future<String?> defaultEsDeSystemFolder({
    required String romRootHandle,
    required EsDeSystem system,
  }) async {
    final raw = await _channel.invokeMethod<Object?>(
      'defaultEsDeSystemFolder',
      {'romRootHandle': romRootHandle, 'system': _systemToJson(system)},
    );
    return raw as String?;
  }

  RomEntry _romEntry(Map raw) => RomEntry(
    relativePath: raw['relativePath'] as String? ?? '',
    fileName: raw['fileName'] as String? ?? '',
    sourcePath: raw['sourcePath'] as String?,
    sizeBytes: _asInt(raw['sizeBytes']),
  );

  OperationEvent _operationEvent(Map raw, int fallbackTotal) {
    final wire = operationEventFromJson(raw.cast<String, Object?>());
    return OperationEvent(
      completed: wire.completed,
      total: wire.total == 0 ? fallbackTotal : wire.total,
      current: wire.current,
      currentProgress: wire.currentProgress,
      spaceSavedBytes: wire.spaceSavedBytes,
      stopped: wire.stopped,
      finished: wire.finished,
      error: wire.error == null ? null : StateError(wire.error!),
    );
  }

  List<EsDeSystem> _systems(Object? raw) {
    if (raw is! List) return const [];
    return raw.whereType<Map>().map(_systemFromJson).toList();
  }

  EsDeSystem _systemFromJson(Map raw) => EsDeSystem(
    name: raw['name'] as String? ?? '',
    fullName: raw['fullName'] as String? ?? raw['name'] as String? ?? '',
    defaultPath: raw['defaultPath'] as String? ?? '',
    romFolder: raw['romFolder'] as String? ?? '',
  );

  Map<String, Object?> _systemToJson(EsDeSystem system) => {
    'name': system.name,
    'fullName': system.fullName,
    'defaultPath': system.defaultPath,
    'romFolder': system.romFolder,
  };

  int _asInt(Object? value, [int fallback = 0]) =>
      value is num ? value.toInt() : int.tryParse('$value') ?? fallback;
}
