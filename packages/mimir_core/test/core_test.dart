import 'dart:typed_data';

import 'package:mimir_core/mimir_core.dart';
import 'package:test/test.dart';

RomEntry entry(String path, {int sizeBytes = 0, String? sourcePath}) =>
    RomEntry(
      relativePath: path,
      fileName: RelativePaths.nameOf(path),
      sizeBytes: sizeBytes,
      sourcePath: sourcePath,
    );

void main() {
  group('RomScanner', () {
    test('detects common disc patterns', () {
      final result = RomScanner.scan([
        entry('psx/Final Fantasy VII (Disc 1).chd'),
        entry('psx/Final Fantasy VII (Disc 2).chd'),
        entry('psx/Final Fantasy VII (Disc 3).chd'),
      ]);
      expect(result.discSets, hasLength(1));
      expect(result.discSets.single.title, 'Final Fantasy VII');
    });

    test('ignores single disc games', () {
      final result = RomScanner.scan([
        entry('psx/Metal Gear Solid (Disc 1).chd'),
        entry('psx/Crash Bandicoot.chd'),
      ]);
      expect(result.discSets, isEmpty);
    });

    test('supports of patterns and strips trailing separators', () {
      expect(RomScanner.parseDisc('Game (1 of 2).cue')!.title, 'Game');
      expect(RomScanner.parseDisc('Game - Disc 2.bin')!.discNumber, 2);
    });
  });

  group('ChangePlanner', () {
    test('builds ES-DE folder-as-file plan', () {
      final plan = ChangePlanner.buildPlan(
        scanResult: RomScanner.scan([
          entry('psx/Xenogears (Disc 1).chd'),
          entry('psx/Xenogears (Disc 2).chd'),
        ]),
        preset: FrontendPreset.esDe,
      );
      expect(plan.conflicts, isEmpty);
      expect(plan.changes.single.detailPath, 'psx/Xenogears.m3u/Xenogears.m3u');
      expect(plan.operations, hasLength(4));
    });

    test('keeps discs in place for other frontends', () {
      final plan = ChangePlanner.buildPlan(
        scanResult: RomScanner.scan([
          entry('saturn/Panzer Dragoon Saga (Disc 1).cue'),
          entry('saturn/Panzer Dragoon Saga (Disc 1).bin'),
          entry('saturn/Panzer Dragoon Saga (Disc 2).cue'),
          entry('saturn/Panzer Dragoon Saga (Disc 2).bin'),
        ]),
        preset: FrontendPreset.other,
      );
      final playlist = plan.operations.single as WriteTextFile;
      expect(
        playlist.contents,
        'Panzer Dragoon Saga (Disc 1).cue\nPanzer Dragoon Saga (Disc 2).cue',
      );
    });

    test(
      'uses hidden source paths while writing output at the visible folder',
      () {
        final plan = ChangePlanner.buildPlan(
          scanResult: RomScanner.scan([
            entry(
              'psx/Parasite Eve Disc 1.chd',
              sourcePath: 'psx/.imports/Parasite Eve Disc 1.chd',
            ),
            entry(
              'psx/Parasite Eve Disc 2.chd',
              sourcePath: 'psx/.imports/Parasite Eve Disc 2.chd',
            ),
          ]),
          preset: FrontendPreset.esDe,
        );
        expect(
          (plan.operations[1] as MoveFile).sourcePath,
          'psx/.imports/Parasite Eve Disc 1.chd',
        );
      },
    );

    test('skips only conflicting sets', () {
      final plan = ChangePlanner.buildPlan(
        scanResult: RomScanner.scan([
          entry('psx/Xenogears (Disc 1).chd'),
          entry('psx/Xenogears (Disc 2).chd'),
          entry('psx/Xenogears.m3u'),
          entry('psx/Grandia (Disc 1).chd'),
          entry('psx/Grandia (Disc 2).chd'),
        ]),
        preset: FrontendPreset.other,
      );
      expect(plan.conflicts, [
        'Skipped Xenogears: target already exists: psx/Xenogears.m3u',
      ]);
      expect(plan.changes.single.title, 'Grandia');
    });

    test('handles PS2 without creating a playlist', () {
      final plan = ChangePlanner.buildPlan(
        scanResult: RomScanner.scan([
          entry('ps2/Xenosaga Disc 1.iso'),
          entry('ps2/Xenosaga Disc 2.iso'),
        ]),
        preset: FrontendPreset.esDe,
      );
      expect(plan.operations.whereType<WriteTextFile>(), isEmpty);
      expect(plan.changes.single.targetFiles, [
        'ps2/Xenosaga/Xenosaga Disc 1.iso',
        'ps2/Xenosaga/Xenosaga Disc 2.iso',
      ]);
    });
  });

  group('conversion planners', () {
    test('plans CHD and marks existing output', () {
      final plan = ChdPlanner.buildPlan(
        entries: [entry('psp/Patapon.iso'), entry('psp/Patapon.CHD')],
        system: ChdSystem.playStationPortable,
        discType: ChdDiscType.dvd,
        deleteOriginalFiles: true,
      );
      expect(plan.changes.single.targetAlreadyExists, isTrue);
      expect(
        (plan.operations.single as ConvertToChd).deleteOriginalFiles,
        isTrue,
      );
    });

    test('filters CHD systems and carries input size', () {
      final plan = ChdPlanner.buildPlan(
        entries: [
          entry('ps2/Shadow.iso', sizeBytes: 1234567),
          entry('ps2/Other.cue'),
        ],
        system: ChdSystem.playStation2,
        discType: ChdDiscType.dvd,
        deleteOriginalFiles: false,
      );
      expect(plan.changes.single.sourceSizeBytes, 1234567);
    });

    test('plans RVZ, ZCCI and NSP inputs', () {
      final rvz = NativeConverterPlanner.buildPlan(
        entries: [entry('wii/Metroid.iso'), entry('wii/Metroid.rvz')],
        tool: ConverterTool.dolphinRvz,
      );
      expect(rvz.changes.single.targetAlreadyExists, isTrue);
      final zcci = NativeConverterPlanner.buildPlan(
        entries: [
          entry('3ds/Game.3ds'),
          entry('3ds/Cart.cci'),
          entry('3ds/Homebrew.3dsx'),
        ],
        tool: ConverterTool.azaharZcci,
      );
      expect(zcci.changes.map((change) => change.detailPath), [
        '3ds/Cart.zcci',
        '3ds/Game.zcci',
      ]);
      final nsp = NativeConverterPlanner.buildPlan(
        entries: [entry('switch/Game.nsz'), entry('switch/Update.NSZ')],
        tool: ConverterTool.nszNsp,
      );
      expect(nsp.changes.map((change) => change.detailPath), [
        'switch/Game.nsp',
        'switch/Update.nsp',
      ]);
    });

    test('plans only whitelisted ROM zip inputs', () {
      final plan = RomZipperPlanner.buildPlan([
        entry('nds/Mario Kart DS.nds'),
        entry('gba/Metroid Fusion.gba'),
        entry('psx/Crash Bandicoot.bin'),
      ]);
      expect(plan.changes.map((change) => change.detailPath).toSet(), {
        'nds/Mario Kart DS.zip',
        'gba/Metroid Fusion.zip',
      });
    });
  });

  group('Vita and SFO', () {
    test('sanitizes Vita names and falls back on collision', () {
      final plan = VitaAppIdPlanner.buildPlan(
        apps: const [
          VitaApp(
            titleId: 'PCSA00123',
            title: 'Chaos/Child',
            sourcePath: 'app/PCSA00123',
          ),
          VitaApp(
            titleId: 'PCSB00999',
            title: 'Chaos:Child',
            sourcePath: 'app/PCSB00999',
          ),
        ],
      );
      expect(plan.changes.map((change) => change.detailPath), [
        'Chaos Child.psvita',
        'Chaos Child [PCSB00999].psvita',
      ]);
    });

    test('writes exact DPT contents', () {
      final plan = VitaAppIdPlanner.buildPlan(
        apps: const [
          VitaApp(
            titleId: 'PCSA00123',
            title: 'Persona 4 Golden',
            sourcePath: 'shortcut-db',
          ),
        ],
        format: VitaShortcutFormat.dpt,
      );
      expect(
        (plan.operations.single as WriteTextFile).contents,
        '[vita_game_id]PCSA00123',
      );
    });

    test('parses UTF-8 SFO fields and rejects malformed input', () {
      final bytes = _buildParamSfo({
        'TITLE_ID': 'PCSE00890',
        'TITLE': '10 Second Ninja X',
      });
      final parsed = ParamSfoParser.parse(bytes);
      expect(parsed['TITLE_ID'], 'PCSE00890');
      expect(parsed['TITLE'], '10 Second Ninja X');
      expect(
        () => ParamSfoParser.parse([1, 2, 3]),
        throwsA(isA<FormatException>()),
      );
    });
  });

  test('round-trips a versioned operation plan', () {
    final original = RomZipperPlanner.buildPlan([entry('gba/Metroid.gba')]);
    final restored = planFromJson(planToJson(original));
    expect(restored.mode, original.mode);
    expect(restored.changes.single.detailPath, 'gba/Metroid.zip');
    expect(restored.operations.single, isA<ZipFile>());
  });

  test('round-trips versioned operation events', () {
    const original = OperationEventData(
      completed: 2,
      total: 4,
      current: 'Game.iso',
      currentProgress: .5,
      spaceSavedBytes: 123,
    );
    final restored = operationEventFromJson(operationEventToJson(original));
    expect(restored.completed, 2);
    expect(restored.currentProgress, .5);
    expect(restored.spaceSavedBytes, 123);
  });
}

List<int> _buildParamSfo(Map<String, String> fields) {
  final keys = <int>[];
  final keyBytes = <int>[];
  final valueBytes = <int>[];
  final entries = <List<int>>[];
  for (final field in fields.entries) {
    keys.add(keyBytes.length);
    keyBytes.addAll(field.key.codeUnits);
    keyBytes.add(0);
    final valueOffset = valueBytes.length;
    final encoded = [...field.value.codeUnits, 0];
    valueBytes.addAll(encoded);
    final entry = ByteData(16)..setUint16(0, keys.last, Endian.little);
    entry.setUint16(2, 0x0204, Endian.little);
    entry.setUint32(4, encoded.length, Endian.little);
    entry.setUint32(8, encoded.length, Endian.little);
    entry.setUint32(12, valueOffset, Endian.little);
    entries.add(entry.buffer.asUint8List());
  }
  final keyStart = 20 + entries.length * 16;
  final dataStart = keyStart + keyBytes.length;
  final output = BytesBuilder();
  final header = ByteData(20)
    ..setUint32(0, 0x46535000, Endian.little)
    ..setUint32(4, 0x00000101, Endian.little)
    ..setUint32(8, keyStart, Endian.little)
    ..setUint32(12, dataStart, Endian.little)
    ..setUint32(16, entries.length, Endian.little);
  output.add(header.buffer.asUint8List());
  for (final entry in entries) {
    output.add(entry);
  }
  output.add(Uint8List.fromList(keyBytes));
  output.add(Uint8List.fromList(valueBytes));
  return output.takeBytes();
}
