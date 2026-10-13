import 'dart:io';
import 'package:archive/archive_io.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mimir/domain/models.dart';
import 'package:mimir/domain/planners.dart';
import 'package:mimir/platform/storage.dart';
import 'package:mimir/platform/converters.dart';
import 'package:mimir/platform/executor.dart';

class FakeConverter implements ConverterService {
  bool fail = false;
  bool cancel = false;
  @override
  Future<String?> unavailableReason(Converter converter) async => null;
  @override
  Future<void> convert(FileOperation operation, String source, String output, String keys, Cancellation cancellation, void Function(double) progress) async {
    await File(output).writeAsString('converted');
    if (cancel) cancellation.immediate = true;
    if (fail) throw StateError('converter failed');
  }
}

void main() {
  late Directory folder;
  late StorageRoot root;
  late DesktopStorage storage;
  late FakeConverter converter;
  late PlanExecutor executor;
  setUp(() async {
    folder = await Directory.systemTemp.createTemp('mimir-test-');
    root = StorageRoot(folder.path, 'test'); storage = DesktopStorage(); converter = FakeConverter(); executor = PlanExecutor(storage, converter, '');
  });
  tearDown(() async { await folder.delete(recursive: true); });
  Future<List<ItemResult>> run(List<PlannedChange> changes, {Cancellation? cancellation, bool overwrite = false}) => executor.run(root, changes, cancellation ?? Cancellation(), overwrite: overwrite, progress: (_, _, _, _) {});
  test('ZIP is readable and retains source', () async {
    await File('${folder.path}/Game.gba').writeAsBytes([1, 2, 3, 4]);
    final plan = planZip(await storage.scan(root, Cancellation()));
    expect((await run(plan.changes)).single.status, ItemStatus.completed);
    final archive = ZipDecoder().decodeBytes(await File('${folder.path}/Game.zip').readAsBytes());
    expect(archive.single.name, 'Game.gba'); expect(archive.single.content, [1, 2, 3, 4]); expect(await File('${folder.path}/Game.gba').exists(), isTrue);
  });
  test('rechecks target created after preview', () async {
    final plan = planVita(const [VitaApp('PCSE00001', 'Game')], [], ShortcutFormat.psvita);
    await File('${folder.path}/Game.psvita').writeAsString('existing');
    expect((await run(plan.changes)).single.status, ItemStatus.skipped);
    expect(await File('${folder.path}/Game.psvita').readAsString(), 'existing');
  });
  test('failed converter preserves existing output and source', () async {
    await File('${folder.path}/Game.iso').writeAsString('source');
    await File('${folder.path}/Game.chd').writeAsString('original output');
    converter.fail = true;
    final plan = planConverter(await storage.scan(root, Cancellation()), Converter.chd, deleteSource: true);
    expect((await run(plan.changes, overwrite: true)).single.status, ItemStatus.failed);
    expect(await File('${folder.path}/Game.iso').readAsString(), 'source');
    expect(await File('${folder.path}/Game.chd').readAsString(), 'original output');
  });
  test('immediate cancellation discards staged output', () async {
    await File('${folder.path}/Game.iso').writeAsString('source'); converter.cancel = true;
    final plan = planConverter(await storage.scan(root, Cancellation()), Converter.chd, deleteSource: true);
    expect((await run(plan.changes)).single.status, ItemStatus.cancelled);
    expect(await File('${folder.path}/Game.iso').exists(), isTrue); expect(await File('${folder.path}/Game.chd').exists(), isFalse);
  });
  test('stop after current commits first output and cancels the rest', () async {
    final cancellation = Cancellation();
    final plan = planVita(const [VitaApp('PCSE00001', 'A'), VitaApp('PCSE00002', 'B')], [], ShortcutFormat.psvita);
    final result = await executor.run(root, plan.changes, cancellation, progress: (done, _, _, _) { if (done == 1) cancellation.afterCurrent = true; });
    expect(result.map((r) => r.status), [ItemStatus.completed, ItemStatus.cancelled]);
    expect(await File('${folder.path}/A.psvita').exists(), isTrue); expect(await File('${folder.path}/B.psvita').exists(), isFalse);
  });
  test('transaction rolls back moves after subsequent write fails', () async {
    await File('${folder.path}/source').writeAsString('ROM');
    final result = await run([const PlannedChange('transaction', [FileOperation(OperationKind.move, 'target', source: 'source'), FileOperation(OperationKind.text, 'missing/fail', contents: 'text')])]);
    expect(result.single.status, ItemStatus.failed); expect(await File('${folder.path}/source').readAsString(), 'ROM'); expect(await File('${folder.path}/target').exists(), isFalse);
  });
  test('storage refuses traversal', () async {
    expect(() => storage.read(root, '../outside'), throwsArgumentError);
    expect(() => storage.read(root, '/absolute'), throwsArgumentError);
  });
}
