import 'dart:convert';
import 'dart:io';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'storage.dart';

class SettingsService {
  SettingsService(this.preferences, this.support);
  final SharedPreferences preferences;
  final Directory support;
  String get keysPath => p.join(support.path, 'nsz', 'prod.keys');
  static Future<SettingsService> load() async {
    final preferences = await SharedPreferences.getInstance();
    final support = await getApplicationSupportDirectory();
    await support.create(recursive: true);
    final service = SettingsService(preferences, support);
    if (Platform.isAndroid && !preferences.containsKey('legacyMigration')) {
      final values = await nativeChannel.invokeMapMethod<String, dynamic>('legacySettings') ?? {};
      for (final entry in values.entries) {
        if (entry.value is bool) await preferences.setBool(entry.key, entry.value as bool);
        if (entry.value is String) await preferences.setString(entry.key, entry.value as String);
      }
      await nativeChannel.invokeMethod<void>('migrateKeys', {'destination': service.keysPath});
      await preferences.setBool('legacyMigration', true);
    }
    return service;
  }
  StorageRoot? root(String key) {
    final value = preferences.getString(key);
    if (value == null) return null;
    try { return StorageRoot.fromJson(jsonDecode(value) as Map<String, dynamic>); } catch (_) { return null; }
  }
  Future<void> saveRoot(String key, StorageRoot value) => preferences.setString(key, jsonEncode(value.toJson()));
  Future<void> importKeys(String source) async {
    final target = File(keysPath);
    await target.parent.create(recursive: true);
    final temporary = File('$keysPath.importing');
    try {
      await File(source).copy(temporary.path);
      if (await temporary.length() == 0) throw const FormatException('The selected prod.keys file is empty');
      if (Platform.isWindows && await target.exists()) {
        final backup = File('$keysPath.backup');
        if (await backup.exists()) throw const FileSystemException('A previous keys backup needs recovery');
        await target.rename(backup.path);
        try { await temporary.rename(target.path); } catch (_) { await backup.rename(target.path); rethrow; }
        await backup.delete();
      } else {
        await temporary.rename(target.path);
      }
    } finally { if (await temporary.exists()) await temporary.delete(); }
  }
}
