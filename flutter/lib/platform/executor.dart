import 'dart:convert';
import 'dart:io';
import 'dart:isolate';
import 'package:archive/archive_io.dart';
import 'package:path/path.dart' as p;
import '../domain/models.dart';
import 'converters.dart';
import 'storage.dart';

List<String> descriptorTracks(String text, String extension) {
  final tracks = <String>[];
  for (final line in const LineSplitter().convert(text)) {
    if (extension == 'cue') {
      final match = RegExp(r'^\s*FILE\s+(?:"([^"]+)"|(\S+))', caseSensitive: false).firstMatch(line);
      if (match != null) tracks.add(match[1] ?? match[2]!);
    } else if (extension == 'gdi') {
      final match = RegExp(r'^\s*\d+\s+\d+\s+\d+\s+\d+\s+(?:"([^"]+)"|(\S+))\s+\d+\s*$').firstMatch(line);
      if (match != null) tracks.add(match[1] ?? match[2]!);
    }
  }
  for (final track in tracks) {
    if (track.contains('/') || track.contains('\\') || track == '.' || track == '..' || track.contains(':')) {
      throw const FormatException('Descriptor references a track outside its folder; conversion stopped.');
    }
  }
  return tracks.toSet().toList();
}

class PlanExecutor {
  PlanExecutor(this.storage, this.converters, this.keysPath);
  final StorageService storage;
  final ConverterService converters;
  final String keysPath;

  Future<List<ItemResult>> run(StorageRoot root, List<PlannedChange> changes, Cancellation cancellation,
    {bool overwrite = false, required void Function(int, int, double, String) progress}) async {
    final results = <ItemResult>[];
    for (var index = 0; index < changes.length; index++) {
      final change = changes[index];
      if (cancellation.requested) {
        results.addAll(changes.skip(index).map((c) => ItemResult(c.title, ItemStatus.cancelled)));
        break;
      }
      progress(index, changes.length, 0, change.title);
      try {
        for (final op in change.operations) {
          if (await storage.exists(root, op.target) && !(overwrite &&
              (op.kind == OperationKind.convert || op.kind == OperationKind.text))) {
            throw _Conflict('Target already exists: ${op.target}');
          }
          if (op.source != null && !await storage.exists(root, op.source!)) throw FileSystemException('Source is unavailable', op.source);
        }
        await _apply(root, change, cancellation, overwrite,
          (fraction) => progress(index, changes.length, fraction, change.title));
        results.add(ItemResult(change.title, ItemStatus.completed));
      } on _Conflict catch (error) {
        results.add(ItemResult(change.title, ItemStatus.skipped, error.message));
      } on OperationCancelled {
        results.add(ItemResult(change.title, ItemStatus.cancelled));
      } catch (error) {
        results.add(ItemResult(change.title, ItemStatus.failed, error.toString()));
      }
      progress(index + 1, changes.length, 0, change.title);
    }
    return results;
  }

  Future<void> _apply(StorageRoot root, PlannedChange change, Cancellation cancellation,
    bool overwrite, void Function(double) progress) async {
    final workspace = await Directory.systemTemp.createTemp('mimir-');
    final rollback = <Future<void> Function()>[];
    final deleteAfterCommit = <String>[];
    var retainWorkspace = false;
    try {
      for (var i = 0; i < change.operations.length; i++) {
        cancellation.check();
        final op = change.operations[i];
        switch (op.kind) {
          case OperationKind.directory:
            await storage.mkdir(root, op.target);
            rollback.add(() => storage.rmdir(root, op.target));
          case OperationKind.move:
            await storage.move(root, op.source!, op.target);
            rollback.add(() => storage.move(root, op.target, op.source!));
          case OperationKind.text:
            final file = File(p.join(workspace.path, 'text-$i'));
            await file.writeAsString(op.contents!, flush: true);
            await _commit(root, op.target, file.path, workspace, i, overwrite, rollback);
          case OperationKind.zip:
            final source = p.join(workspace.path, p.basename(op.source!));
            final output = p.join(workspace.path, 'archive-$i.zip');
            await storage.export(root, op.source!, source);
            cancellation.check();
            // Use a cancellable isolate, rather than loading a ROM into memory.
            await _zip(source, output, cancellation);
            cancellation.check();
            await _commit(root, op.target, output, workspace, i, false, rollback);
          case OperationKind.convert:
            final source = p.join(workspace.path, p.basename(op.source!));
            final output = p.join(workspace.path, p.basename(op.target));
            final missing = await converters.unavailableReason(op.converter!);
            if (missing != null) throw StateError(missing);
            await storage.export(root, op.source!, source);
            final ext = op.source!.split('.').last.toLowerCase();
            final tracks = ext == 'cue' || ext == 'gdi'
              ? descriptorTracks(await File(source).readAsString(), ext) : <String>[];
            final parent = op.source!.contains('/') ? op.source!.substring(0, op.source!.lastIndexOf('/')) : '';
            for (final track in tracks) {
              cancellation.check();
              await storage.export(root, joinRelative(parent, track), p.join(workspace.path, track));
            }
            await converters.convert(op, source, output, keysPath, cancellation, progress);
            cancellation.check();
            if (!await File(output).exists() || await File(output).length() == 0) throw const FileSystemException('Empty converter output');
            await _commit(root, op.target, output, workspace, i, overwrite, rollback);
            if (op.deleteSource) {
              deleteAfterCommit.add(op.source!);
              deleteAfterCommit.addAll(tracks.map((t) => joinRelative(parent, t)));
            }
        }
      }
    } catch (error) {
      final failures = <String>[];
      for (final undo in rollback.reversed) {
        try { await undo(); } catch (rollbackError) { failures.add(rollbackError.toString()); }
      }
      if (failures.isNotEmpty) {
        retainWorkspace = true;
        throw StateError('$error. Some changes could not be restored: ${failures.join('; ')}. Recovery files retained at ${workspace.path}');
      }
      rethrow;
    } finally {
      if (!retainWorkspace && await workspace.exists()) await workspace.delete(recursive: true);
    }
    // Never remove an output once source cleanup has begun.
    for (final source in deleteAfterCommit) {
      try { await storage.delete(root, source); } catch (error) {
        throw StateError('Output created successfully, but source cleanup was incomplete: $source ($error)');
      }
    }
  }

  Future<void> _commit(StorageRoot root, String target, String local, Directory workspace,
    int index, bool overwrite, List<Future<void> Function()> rollback) async {
    final previous = await storage.exists(root, target);
    final backup = p.join(workspace.path, 'previous-$index');
    if (previous) {
      if (!overwrite) throw _Conflict('Target already exists: $target');
      await storage.export(root, target, backup);
    }
    await storage.commit(root, target, local, overwrite: overwrite);
    rollback.add(previous
      ? () => storage.commit(root, target, backup, overwrite: true)
      : () => storage.delete(root, target));
  }
}

class _Conflict implements Exception {
  _Conflict(this.message);
  final String message;
}

Future<void> _zipWorker(({String source, String output, SendPort? port}) request) async {
  try {
    final encoder = ZipFileEncoder();
    encoder.create(request.output);
    await encoder.addFile(File(request.source), p.basename(request.source));
    await encoder.close();
    request.port?.send(null);
  } catch (error) { request.port?.send(error.toString()); }
}

Future<void> _zip(String source, String output, Cancellation cancellation) async {
  final receive = ReceivePort();
  final isolate = await Isolate.spawn(_zipWorker, (source: source, output: output, port: receive.sendPort));
  final completion = receive.first;
  final timer = Stream<void>.periodic(const Duration(milliseconds: 100));
  final subscription = timer.listen((_) {
    if (cancellation.immediate) { isolate.kill(priority: Isolate.immediate); receive.close(); }
  });
  try {
    final error = await completion;
    cancellation.check();
    if (error != null) throw StateError(error.toString());
  } catch (_) { cancellation.check(); rethrow; }
  finally { await subscription.cancel(); receive.close(); isolate.kill(priority: Isolate.immediate); }
}
