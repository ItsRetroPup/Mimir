import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as p;
import 'package:xml/xml.dart';

import 'platform_adapter.dart';

class EsDeRepository {
  static const customSystemsDir = 'custom_systems';
  static const systemsFile = 'es_systems.xml';
  static const findRulesFile = 'es_find_rules.xml';
  static const officialSystemsUrl =
      'https://gitlab.com/es-de/emulationstation-de/-/raw/stable-3.0/resources/systems/android/es_systems.xml';
  static const latestSystemsUrl =
      'https://raw.githubusercontent.com/GlazedBelmont/es-de-android-custom-systems/main/es_systems.xml';
  static const latestFindRulesUrl =
      'https://raw.githubusercontent.com/GlazedBelmont/es-de-android-custom-systems/main/es_find_rules.xml';

  Future<List<EsDeSystem>> loadInstalled(
    String rootHandle, {
    String? romRootHandle,
  }) async {
    final file = File(p.join(rootHandle, customSystemsDir, systemsFile));
    if (!await file.exists()) return const [];
    final systems = _parseSystems(await file.readAsString());
    if (romRootHandle == null) return systems;
    final folders = await scanRomFolders(romRootHandle);
    final allowed = folders.map((folder) => folder.toLowerCase()).toSet();
    return systems
        .where((system) => allowed.contains(system.name.toLowerCase()))
        .toList();
  }

  Future<List<EsDeSystem>> fetchLatest() async {
    final official = await _download(officialSystemsUrl);
    final custom = await _download(latestSystemsUrl);
    return _parseSystems(_mergeCatalogXml(official, custom));
  }

  Future<List<String>> scanRomFolders(String rootHandle) async {
    final directory = Directory(rootHandle);
    if (!await directory.exists()) return const [];
    final folders = <String>[];
    await for (final entity in directory.list(followLinks: false)) {
      if (entity is Directory) folders.add(p.basename(entity.path));
    }
    folders.sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase()));
    return folders;
  }

  List<EsDeSystem> systemsForRomFolders(
    List<EsDeSystem> catalog,
    List<String> folders,
  ) {
    final byName = {
      for (final system in catalog) system.name.toLowerCase(): system,
    };
    return folders
        .map((folder) {
          final system = byName[folder.toLowerCase()];
          return system?.copyWith(
            defaultPath: '%ROMPATH%/$folder',
            romFolder: folder,
          );
        })
        .whereType<EsDeSystem>()
        .toList();
  }

  Future<int> installLatest(String rootHandle, List<EsDeSystem> systems) async {
    final merged = _mergeCatalogXml(
      await _download(officialSystemsUrl),
      await _download(latestSystemsUrl),
    );
    final systemsXml = _buildInstalledXml(merged, systems);
    await _writeInstall(
      rootHandle,
      systemsXml,
      await _download(latestFindRulesUrl),
    );
    return _parseSystems(systemsXml).length;
  }

  Future<int> installCurrent(
    String rootHandle,
    List<EsDeSystem> systems,
  ) async {
    final custom = Directory(p.join(rootHandle, customSystemsDir));
    await custom.create(recursive: true);
    final current = File(p.join(custom.path, systemsFile));
    final systemsXml = await current.exists()
        ? _buildInstalledXml(await current.readAsString(), systems)
        : _buildInstalledXml(
            _mergeCatalogXml(
              await _download(officialSystemsUrl),
              await _download(latestSystemsUrl),
            ),
            systems,
          );
    await File(p.join(custom.path, systemsFile)).writeAsString(systemsXml);
    final findRules = File(p.join(custom.path, findRulesFile));
    if (!await findRules.exists()) {
      await findRules.writeAsString(await _download(latestFindRulesUrl));
    }
    return _parseSystems(systemsXml).length;
  }

  Future<void> _writeInstall(
    String rootHandle,
    String systemsXml,
    String findRulesXml,
  ) async {
    final custom = Directory(p.join(rootHandle, customSystemsDir));
    await custom.create(recursive: true);
    await File(p.join(custom.path, systemsFile)).writeAsString(systemsXml);
    await File(p.join(custom.path, findRulesFile)).writeAsString(findRulesXml);
  }

  Future<String> _download(String url) async {
    final client = HttpClient();
    try {
      final request = await client.getUrl(Uri.parse(url));
      request.headers.set(
        HttpHeaders.userAgentHeader,
        'Mimir/ES-DE-custom-systems',
      );
      final response = await request.close();
      if (response.statusCode < 200 || response.statusCode > 299) {
        throw StateError(
          'ES-DE download failed with HTTP ${response.statusCode}.',
        );
      }
      return utf8.decode(
        await response.fold<List<int>>(
          [],
          (bytes, chunk) => bytes..addAll(chunk),
        ),
      );
    } finally {
      client.close(force: true);
    }
  }

  List<EsDeSystem> _parseSystems(String xml) {
    final document = XmlDocument.parse(xml);
    return document
        .findAllElements('system')
        .map((element) {
          final name = element.getElement('name')?.innerText.trim() ?? '';
          final fullName =
              (element.getElement('fullname')?.innerText.trim().isNotEmpty ??
                  false)
              ? element.getElement('fullname')!.innerText.trim()
              : name;
          final path = element.getElement('path')?.innerText.trim() ?? '';
          if (name.isEmpty || path.isEmpty) return null;
          return EsDeSystem(
            name: name,
            fullName: fullName,
            defaultPath: path,
            romFolder: path
                .replaceFirst('%ROMPATH%/', '')
                .replaceFirst('%ROMPATH%', ''),
          );
        })
        .whereType<EsDeSystem>()
        .fold<List<EsDeSystem>>([], (result, system) {
          if (!result.any((item) => item.name == system.name)) {
            result.add(system);
          }
          return result;
        });
  }

  String _mergeCatalogXml(String baseXml, String customXml) {
    final base = XmlDocument.parse(baseXml);
    final custom = XmlDocument.parse(customXml);
    final baseRoot = base.rootElement;
    final baseByName = {
      for (final element in base.findAllElements('system'))
        element.getElement('name')?.innerText ?? '': element,
    };
    for (final customSystem in custom.findAllElements('system')) {
      final name = customSystem.getElement('name')?.innerText;
      if (name == null || name.isEmpty) continue;
      final imported = XmlElement(
        XmlName('system'),
        customSystem.attributes,
        customSystem.children.map((child) => child.copy()).toList(),
      );
      final existing = baseByName[name];
      if (existing == null) {
        baseRoot.children.add(imported);
      } else {
        final index = baseRoot.children.indexOf(existing);
        baseRoot.children[index] = imported;
      }
    }
    return base.toXmlString(pretty: true, indent: '  ');
  }

  String _buildInstalledXml(String xml, List<EsDeSystem> systems) {
    final document = XmlDocument.parse(xml);
    final selected = {for (final system in systems) system.name: system};
    for (final system in document.findAllElements('system').toList().reversed) {
      final name = system.getElement('name')?.innerText;
      final selectedSystem = name == null ? null : selected[name];
      if (selectedSystem == null) {
        system.parent?.children.remove(system);
        continue;
      }
      final path = system.getElement('path');
      if (path != null) {
        path.innerText = selectedSystem.isDefault
            ? '%ROMPATH%/${selectedSystem.romFolder}'
            : selectedSystem.romFolder;
      }
    }
    return document.toXmlString(pretty: true, indent: '  ');
  }
}
