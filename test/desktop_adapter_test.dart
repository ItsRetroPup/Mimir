import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mimir_core/mimir_core.dart';
import 'package:path/path.dart' as p;

import 'package:mimir_flutter/platform/desktop_adapter.dart';
import 'package:mimir_flutter/platform/platform_adapter.dart';

void main() {
  late Directory root;
  late DesktopPlatformAdapter adapter;

  setUp(() async {
    root = await Directory.systemTemp.createTemp('mimir-desktop-test-');
    adapter = DesktopPlatformAdapter();
  });

  tearDown(() async {
    if (await root.exists()) await root.delete(recursive: true);
  });

  test('scans, previews, and applies a multi-disc organizer plan', () async {
    final game = Directory(p.join(root.path, 'psx'))..createSync();
    await File(
      p.join(game.path, 'Example (Disc 1).cue'),
    ).writeAsString('FILE "disc1.bin" BINARY');
    await File(p.join(game.path, 'disc1.bin')).writeAsString('track 1');
    await File(
      p.join(game.path, 'Example (Disc 2).cue'),
    ).writeAsString('FILE "disc2.bin" BINARY');
    await File(p.join(game.path, 'disc2.bin')).writeAsString('track 2');

    final entries = await adapter.scan(
      rootHandle: root.path,
      request: const ScanRequest(mode: ToolMode.multiDiscOrganizer),
    );
    final plan = ChangePlanner.buildPlan(
      scanResult: RomScanner.scan(entries),
      preset: FrontendPreset.esDe,
    );
    expect(plan.changes, hasLength(1));

    final events = await adapter
        .apply(rootHandle: root.path, plan: plan)
        .toList();

    expect(events.last.finished, isTrue);
    expect(
      File(
        p.join(root.path, 'psx', 'Example.m3u', 'Example.m3u'),
      ).readAsStringSync(),
      contains('Example (Disc 1).cue'),
    );
    expect(File(p.join(game.path, 'disc1.bin')).existsSync(), isTrue);
    expect(
      File(p.join(game.path, 'Example (Disc 1).cue')).existsSync(),
      isFalse,
    );
  });

  test(
    'zips a supported ROM and removes the source only after writing output',
    () async {
      final source = File(p.join(root.path, 'Metroid.NES'))
        ..writeAsStringSync('rom bytes');
      final entries = await adapter.scan(
        rootHandle: root.path,
        request: const ScanRequest(mode: ToolMode.romZipper),
      );
      final plan = RomZipperPlanner.buildPlan(entries);

      await adapter.apply(rootHandle: root.path, plan: plan).drain();

      expect(source.existsSync(), isFalse);
      expect(File(p.join(root.path, 'Metroid.zip')).existsSync(), isTrue);
    },
  );

  test('honors a stop-before-next-operation request', () async {
    final plan = const OperationPlan(
      mode: ToolMode.romZipper,
      changes: [],
      operations: [CreateDirectory('created')],
      conflicts: [],
    );
    final cancellation = CancellationToken()..stopAfterCurrent();

    final events = await adapter
        .apply(rootHandle: root.path, plan: plan, cancellation: cancellation)
        .toList();

    expect(events.single.finished, isTrue);
    expect(events.single.stopped, isTrue);
    expect(Directory(p.join(root.path, 'created')).existsSync(), isFalse);
  });

  test('rejects traversal outside the selected root', () async {
    final plan = const OperationPlan(
      mode: ToolMode.romZipper,
      changes: [],
      operations: [
        WriteTextFile(relativePath: '../outside.txt', contents: 'nope'),
      ],
      conflicts: [],
    );

    expect(
      () => adapter.apply(rootHandle: root.path, plan: plan).toList(),
      throwsA(isA<ArgumentError>()),
    );
  });
}
