import 'dart:typed_data';
import 'package:flutter_test/flutter_test.dart';
import 'package:mimir/domain/models.dart';
import 'package:mimir/domain/planners.dart';
import 'package:mimir/domain/vita.dart';
import 'package:mimir/platform/esde.dart';
import 'package:mimir/platform/executor.dart';

void main() {
  test('disc naming patterns retain region and sort numerically', () {
    expect(parseDisc('Game (USA) (Disc 10).chd')!.number, 10);
    expect(parseDisc('Game [2 of 3].cue')!.title, 'Game');
    expect(parseDisc('Normal.iso'), isNull);
    final plan = planOrganiser(const [RomEntry('psx/Game (Disc 10).chd'), RomEntry('psx/Game (Disc 2).chd')], Frontend.esde);
    expect(plan.changes.single.operations.last.contents, 'Game (Disc 2).chd\nGame (Disc 10).chd');
    expect(plan.changes.single.operations.last.target, 'psx/Game.m3u/Game.m3u');
  });
  test('other preset keeps discs and excludes bin tracks from playlist', () {
    final plan = planOrganiser(const [RomEntry('psx/Game (Disc 1).cue'), RomEntry('psx/Game (Disc 1).bin'), RomEntry('psx/Game (Disc 2).cue')], Frontend.other);
    expect(plan.changes.single.operations, hasLength(1));
    expect(plan.changes.single.operations.single.contents, 'Game (Disc 1).cue\nGame (Disc 2).cue');
  });
  test('PS2 groups discs without writing playlists', () {
    final plan = planOrganiser(const [RomEntry('PS2/Game (Disc 1).iso'), RomEntry('PS2/Game (Disc 2).iso')], Frontend.other);
    expect(plan.changes.single.operations.where((o) => o.kind == OperationKind.text), isEmpty);
    expect(plan.changes.single.operations.first.target, 'PS2/Game');
  });
  test('existing folders cause organiser conflict without blocking other games', () {
    final plan = planOrganiser(const [RomEntry('Game.m3u', isDirectory: true), RomEntry('Game (Disc 1).chd'), RomEntry('Game (Disc 2).chd'),
      RomEntry('Other (Disc 1).chd'), RomEntry('Other (Disc 2).chd')], Frontend.esde);
    expect(plan.conflicts, hasLength(1)); expect(plan.changes.single.title, 'Other');
  });
  test('ZIP uses extension-free target and catches case conflicts and collisions', () {
    final plan = planZip(const [RomEntry('gba/Game.gba'), RomEntry('gba/GAME.ZIP'), RomEntry('nes/Demo.nes'), RomEntry('nes/Demo.fds'), RomEntry('psx/disc.bin')]);
    expect(plan.changes.single.operations.single.target, 'nes/Demo.zip');
    expect(plan.conflicts, hasLength(2));
  });
  test('converter scans root and aliases and exposes existing outputs', () {
    final plan = planConverter(const [RomEntry('root.iso'), RomEntry('PS2/Game.iso'), RomEntry('PS2/GAME.CHD'), RomEntry('other/skip.iso'), RomEntry('PS2/track.bin')], Converter.chd, system: 'PlayStation 2', discType: DiscType.dvd);
    expect(plan.changes, hasLength(2));
    expect(plan.changes.first.targetExists, isTrue);
    expect(plan.changes.first.operations.single.target, 'PS2/GAME.CHD');
    expect(plan.changes.first.operations.single.discType, DiscType.dvd);
  });
  test('Vita TSV title-first format includes titles starting with hash', () {
    final database = parseVitaDatabase('#KILLALLZOMBIES\tPCSB01049\nAnother\tPCSE00001\ninvalid');
    expect(database, hasLength(2)); expect(database.first.title, '#KILLALLZOMBIES');
  });
  test('Vita filename fallback and exact dpt contents preserve compatibility', () {
    final plan = planVita(const [VitaApp('PCSE00001', 'A: Game')], const [RomEntry('A Game.dpt')], ShortcutFormat.dpt);
    expect(plan.changes.single.operations.single.target, 'A Game [PCSE00001].dpt');
    expect(plan.changes.single.operations.single.contents, '[vita_game_id]PCSE00001');
  });
  test('malformed SFO rejects bounds instead of reading arbitrary bytes', () {
    expect(() => parseSfo(Uint8List(10)), throwsFormatException);
    final bytes = Uint8List(20); final data = ByteData.sublistView(bytes);
    data.setUint32(0, 0x46535000, Endian.little); data.setUint32(8, 19, Endian.little); data.setUint32(12, 19, Endian.little); data.setUint32(16, 1000, Endian.little);
    expect(() => parseSfo(bytes), throwsFormatException);
  });
  test('CUE/GDI references accept spaces and refuse traversal', () {
    expect(descriptorTracks('FILE "track 1.bin" BINARY\nFILE "track 2.bin" BINARY', 'cue'), ['track 1.bin', 'track 2.bin']);
    expect(descriptorTracks('2\n1 0 4 2352 "track 1.bin" 0\n2 100 0 2352 track02.raw 0', 'gdi'), ['track 1.bin', 'track02.raw']);
    expect(() => descriptorTracks('FILE "../other.bin" BINARY', 'cue'), throwsFormatException);
  });
  test('ES-DE updates only paths and retains desktop commands', () {
    const xml = '<systemList><system><name>psx</name><fullname>PlayStation</fullname><path>%ROMPATH%/psx</path><command>desktop-command %ROM%</command></system><system><name>unused</name><path>unused</path></system></systemList>';
    final plan = EsDeCatalog(xml, '<emulatorList/>').plan([EsDeSystem('psx', 'PlayStation', '/Games/PSX & more')], []);
    final output = plan.changes.single.operations[1].contents!;
    expect(output, contains('desktop-command %ROM%')); expect(output, contains('/Games/PSX &amp; more')); expect(output, isNot(contains('unused')));
  });
}
