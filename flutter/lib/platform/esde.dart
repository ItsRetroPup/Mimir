import 'dart:io';
import 'dart:convert';
import 'package:xml/xml.dart';
import '../domain/models.dart';

class EsDeSystem {
  EsDeSystem(this.name, this.title, this.path);
  final String name;
  final String title;
  String path;
}

class EsDeCatalog {
  EsDeCatalog(this.systemsXml, this.findRulesXml);
  final String systemsXml;
  final String findRulesXml;
  List<EsDeSystem> get systems => XmlDocument.parse(systemsXml).findAllElements('system')
    .where((node) => node.getElement('name') != null && node.getElement('path') != null)
    .map((node) => EsDeSystem(node.getElement('name')!.innerText,
      node.getElement('fullname')?.innerText ?? node.getElement('name')!.innerText,
      node.getElement('path')!.innerText)).toList();

  OperationPlan plan(List<EsDeSystem> selected, List<RomEntry> existing) {
    final document = XmlDocument.parse(systemsXml);
    final paths = {for (final system in selected) system.name: system.path};
    for (final node in document.findAllElements('system').toList()) {
      final path = paths[node.getElement('name')?.innerText];
      if (path == null) { node.parent?.children.remove(node); } else { node.getElement('path')!.innerText = path; }
    }
    final hasFolder = existing.any((e) => e.path == 'custom_systems' && e.isDirectory);
    final files = ['custom_systems/es_systems.xml', 'custom_systems/es_find_rules.xml'];
    return OperationPlan([PlannedChange('Install ${selected.length} ES-DE systems', [
      if (!hasFolder) const FileOperation(OperationKind.directory, 'custom_systems'),
      FileOperation(OperationKind.text, files[0], contents: document.toXmlString(pretty: true)),
      FileOperation(OperationKind.text, files[1], contents: findRulesXml),
    ], targetExists: existing.any((e) => files.contains(e.path)))]);
  }
}

class EsDeCatalogService {
  static const official = 'https://gitlab.com/es-de/emulationstation-de/-/raw/stable-3.0/resources/systems';
  static const androidCustom = 'https://raw.githubusercontent.com/GlazedBelmont/es-de-android-custom-systems/main';
  Future<EsDeCatalog> fetch() async {
    final platform = Platform.isWindows ? 'windows' : Platform.isMacOS ? 'macos' : Platform.isLinux ? 'unix' : 'android';
    var systems = await _download('$official/$platform/es_systems.xml');
    var rules = await _download('$official/$platform/es_find_rules.xml');
    if (Platform.isAndroid) {
      systems = mergeSystems(systems, await _download('$androidCustom/es_systems.xml'));
      rules = await _download('$androidCustom/es_find_rules.xml');
    }
    // Parse before presenting any install operation.
    XmlDocument.parse(rules);
    XmlDocument.parse(systems);
    return EsDeCatalog(systems, rules);
  }
  Future<String> _download(String url) async {
    final client = HttpClient()..connectionTimeout = const Duration(seconds: 20);
    try {
      final request = await client.getUrl(Uri.parse(url));
      request.headers.set('User-Agent', 'Mimir/ES-DE-systems');
      final response = await request.close().timeout(const Duration(seconds: 60));
      if (response.statusCode != 200) throw HttpException('Unable to download ES-DE definitions (HTTP ${response.statusCode})');
      final bytes = await response.fold<List<int>>([], (all, chunk) => all..addAll(chunk)).timeout(const Duration(seconds: 60));
      return utf8.decode(bytes);
    } finally { client.close(force: true); }
  }
}

String mergeSystems(String base, String custom) {
  final document = XmlDocument.parse(base);
  final byName = {for (final node in document.findAllElements('system')) node.getElement('name')?.innerText: node};
  for (final node in XmlDocument.parse(custom).findAllElements('system')) {
    final existing = byName[node.getElement('name')?.innerText];
    if (existing != null) { existing.replace(node.copy()); }
    else { document.rootElement.children.add(node.copy()); }
  }
  return document.toXmlString(pretty: true);
}
