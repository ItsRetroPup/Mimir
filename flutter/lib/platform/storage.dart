import 'dart:io';
import 'dart:typed_data';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/services.dart';
import 'package:path/path.dart' as p;
import '../domain/models.dart';

const nativeChannel = MethodChannel('pup.app.mimir/native');

class StorageRoot {
  const StorageRoot(this.reference, this.label);
  final String reference;
  final String label;
  Map<String, String> toJson() => {'reference': reference, 'label': label};
  static StorageRoot fromJson(Map<String, dynamic> value) => StorageRoot(value['reference'] as String, value['label'] as String);
}

abstract interface class StorageService {
  Future<StorageRoot?> pickFolder();
  Future<List<RomEntry>> scan(StorageRoot root, Cancellation cancellation, {bool hidden = false});
  Future<bool> exists(StorageRoot root, String relative);
  Future<void> mkdir(StorageRoot root, String relative);
  Future<void> rmdir(StorageRoot root, String relative);
  Future<void> export(StorageRoot root, String relative, String localPath);
  Future<void> commit(StorageRoot root, String relative, String localPath, {bool overwrite = false});
  Future<void> move(StorageRoot root, String source, String target);
  Future<void> delete(StorageRoot root, String relative);
  Future<Uint8List> read(StorageRoot root, String relative);
  Future<String?> absolutePath(StorageRoot root);
}

/// Reject traversal before any adapter touches a user's library.
String safeRelative(String value) {
  if (value.isEmpty || value.startsWith('/') || value.contains('\\') || value.contains(':') ||
      value.split('/').any((s) => s.isEmpty || s == '.' || s == '..')) {
    throw ArgumentError('Invalid library-relative path: $value');
  }
  return value;
}

class DesktopStorage implements StorageService {
  Future<String> _path(StorageRoot root, String relative) async {
    final base = await Directory(root.reference).resolveSymbolicLinks();
    final target = p.joinAll([base, ...safeRelative(relative).split('/')]);
    var ancestor = target;
    while (!await FileSystemEntity.isDirectory(ancestor) && !await FileSystemEntity.isFile(ancestor) && !await FileSystemEntity.isLink(ancestor)) {
      final next = p.dirname(ancestor);
      if (next == ancestor) throw FileSystemException('Folder is unavailable', root.reference);
      ancestor = next;
    }
    final resolved = await FileSystemEntity.isDirectory(ancestor)
        ? await Directory(ancestor).resolveSymbolicLinks()
        : await File(ancestor).resolveSymbolicLinks();
    if (resolved != base && !p.isWithin(base, resolved)) throw FileSystemException('Path leaves selected folder', relative);
    return target;
  }
  @override
  Future<StorageRoot?> pickFolder() async {
    final directory = await getDirectoryPath();
    return directory == null ? null : StorageRoot(directory, p.basename(directory));
  }
  @override
  Future<List<RomEntry>> scan(StorageRoot root, Cancellation cancellation, {bool hidden = false}) async {
    final base = Directory(root.reference);
    final result = <RomEntry>[];
    Future<void> visit(Directory directory) async {
      await for (final entity in directory.list(followLinks: false)) {
        cancellation.check();
        if (entity is Link || (!hidden && p.basename(entity.path).startsWith('.'))) continue;
        final path = p.relative(entity.path, from: base.path).split(p.separator).join('/');
        final stat = await entity.stat();
        result.add(RomEntry(path, size: stat.size, isDirectory: entity is Directory));
        if (entity is Directory) await visit(entity);
      }
    }
    await visit(base);
    result.sort((a, b) => a.path.compareTo(b.path));
    return result;
  }
  @override
  Future<bool> exists(StorageRoot root, String relative) async => await FileSystemEntity.type(await _path(root, relative), followLinks: false) != FileSystemEntityType.notFound;
  @override
  Future<void> mkdir(StorageRoot root, String relative) async { await Directory(await _path(root, relative)).create(recursive: true); }
  @override
  Future<void> rmdir(StorageRoot root, String relative) async {
    final directory = Directory(await _path(root, relative));
    if (await directory.exists() && await directory.list().isEmpty) await directory.delete();
  }
  @override
  Future<void> export(StorageRoot root, String relative, String localPath) async { await File(await _path(root, relative)).copy(localPath); }
  @override
  Future<Uint8List> read(StorageRoot root, String relative) async => File(await _path(root, relative)).readAsBytes();
  @override
  Future<void> delete(StorageRoot root, String relative) async { await File(await _path(root, relative)).delete(); }
  @override
  Future<void> move(StorageRoot root, String source, String target) async {
    if (await exists(root, target)) throw FileSystemException('Target already exists', target);
    await File(await _path(root, source)).rename(await _path(root, target));
  }
  @override
  Future<void> commit(StorageRoot root, String relative, String localPath, {bool overwrite = false}) async {
    final target = await _path(root, relative);
    if (!overwrite && await exists(root, relative)) throw FileSystemException('Target already exists', relative);
    final nonce = '${DateTime.now().microsecondsSinceEpoch}';
    final temporary = File('$target.mimir-$nonce.tmp');
    final backup = File('$target.mimir-$nonce.backup');
    var backedUp = false;
    try {
      await File(localPath).copy(temporary.path);
      if (await temporary.length() != await File(localPath).length()) throw const FileSystemException('Incomplete output copy');
      if (await File(target).exists()) {
        if (!overwrite) throw FileSystemException('Target already exists', relative);
        await File(target).rename(backup.path);
        backedUp = true;
      }
      await temporary.rename(target);
    } catch (_) {
      if (backedUp && !await File(target).exists()) await backup.rename(target);
      rethrow;
    } finally {
      if (await temporary.exists()) await temporary.delete();
    }
    if (backedUp) await backup.delete();
  }
  @override
  Future<String?> absolutePath(StorageRoot root) async => Directory(root.reference).absolute.path;
}

class AndroidStorage implements StorageService {
  Future<T?> _call<T>(String method, StorageRoot root, [Map<String, Object?> args = const {}]) =>
    nativeChannel.invokeMethod<T>(method, {'root': root.reference, ...args});
  @override
  Future<StorageRoot?> pickFolder() async {
    final value = await nativeChannel.invokeMapMethod<String, dynamic>('pickFolder');
    return value == null ? null : StorageRoot.fromJson(value);
  }
  @override
  Future<List<RomEntry>> scan(StorageRoot root, Cancellation cancellation, {bool hidden = false}) async {
    final result = <RomEntry>[];
    Future<void> visit(String parent) async {
      cancellation.check();
      final items = await _call<List<dynamic>>('list', root, {'path': parent}) ?? [];
      for (final raw in items) {
        cancellation.check();
        final item = Map<String, dynamic>.from(raw as Map);
        if (!hidden && (item['name'] as String).startsWith('.')) continue;
        final entry = RomEntry(joinRelative(parent, item['name'] as String), size: (item['size'] as num).toInt(), isDirectory: item['directory'] as bool);
        result.add(entry);
        if (entry.isDirectory) await visit(entry.path);
      }
    }
    await visit('');
    return result;
  }
  @override
  Future<bool> exists(StorageRoot root, String relative) async => await _call<bool>('exists', root, {'path': safeRelative(relative)}) ?? false;
  @override
  Future<void> mkdir(StorageRoot root, String relative) async { await _call<void>('mkdir', root, {'path': safeRelative(relative)}); }
  @override
  Future<void> rmdir(StorageRoot root, String relative) async { await _call<void>('rmdir', root, {'path': safeRelative(relative)}); }
  @override
  Future<void> export(StorageRoot root, String relative, String localPath) async { await _call<void>('export', root, {'path': safeRelative(relative), 'local': localPath}); }
  @override
  Future<void> commit(StorageRoot root, String relative, String localPath, {bool overwrite = false}) async { await _call<void>('commit', root, {'path': safeRelative(relative), 'local': localPath, 'overwrite': overwrite}); }
  @override
  Future<void> move(StorageRoot root, String source, String target) async { await _call<void>('move', root, {'source': safeRelative(source), 'path': safeRelative(target)}); }
  @override
  Future<void> delete(StorageRoot root, String relative) async { await _call<void>('delete', root, {'path': safeRelative(relative)}); }
  @override
  Future<Uint8List> read(StorageRoot root, String relative) async => await _call<Uint8List>('read', root, {'path': safeRelative(relative)}) ?? Uint8List(0);
  @override
  Future<String?> absolutePath(StorageRoot root) => _call<String>('absolutePath', root);
}
