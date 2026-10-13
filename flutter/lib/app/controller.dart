import 'dart:io';
import 'dart:convert';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/legacy.dart';
import '../domain/models.dart';
import '../domain/planners.dart';
import '../domain/vita.dart';
import '../platform/storage.dart';
import '../platform/settings.dart';
import '../platform/converters.dart';
import '../platform/executor.dart';
import '../platform/esde.dart';

final controllerProvider = ChangeNotifierProvider<AppController>((ref) => throw StateError('Controller must be provided at startup'));

class AppController extends ChangeNotifier {
  AppController(this.settings, this.storage, this.converters) {
    root = settings.root('romRoot');
    vitaOutput = settings.root('vitaOutput');
    esdeRoot = settings.root('esdeRoot');
    dark = settings.preferences.getBool('dark') ?? true;
    hidden = settings.preferences.getBool('hidden') ?? false;
    deleteSource = settings.preferences.getBool('deleteSource') ?? false;
    format = settings.preferences.getString('shortcutFormat') == 'dpt' ? ShortcutFormat.dpt : ShortcutFormat.psvita;
  }
  final SettingsService settings;
  final StorageService storage;
  final ConverterService converters;
  Tool tool = Tool.organiser;
  StorageRoot? root, vitaOutput, esdeRoot;
  Frontend frontend = Frontend.esde;
  Converter converter = Converter.chd;
  String system = 'Dreamcast';
  DiscType discType = DiscType.cd;
  ShortcutFormat format = ShortcutFormat.psvita;
  bool dark = true, hidden = false, deleteSource = false, busy = false, running = false;
  String? error;
  String activity = '';
  int completed = 0, total = 0;
  double fraction = 0;
  OperationPlan? plan;
  Tool? planTool;
  StorageRoot? planRoot;
  Set<int> selected = {};
  List<ItemResult> results = [];
  List<VitaApp> database = [], scannedApps = [];
  List<RomEntry> vitaEntries = [];
  Set<String> selectedVita = {};
  Set<String> existingVita = {};
  EsDeCatalog? catalog;
  List<EsDeSystem> esdeSystems = [];
  Cancellation? cancellation;
  Future<void> loadDatabase() async {
    database = parseVitaDatabase(await rootBundle.loadString('assets/vita_shortcuts.tsv'));
    notifyListeners();
  }
  void navigate(Tool value) { tool = value; notifyListeners(); }
  void invalidate() { plan = null; selected = {}; results = []; error = null; notifyListeners(); }
  void configure(VoidCallback update) { if (busy) return; update(); invalidate(); }
  Future<void> setDark(bool value) async { dark = value; notifyListeners(); await settings.preferences.setBool('dark', value); }
  Future<void> setHidden(bool value) async { hidden = value; invalidate(); await settings.preferences.setBool('hidden', value); }
  Future<void> setDeleteSource(bool value) async { deleteSource = value; invalidate(); await settings.preferences.setBool('deleteSource', value); }
  Future<void> setFormat(ShortcutFormat value) async { format = value; existingVita = {}; invalidate(); await settings.preferences.setString('shortcutFormat', value.name); await refreshVita(); }
  Future<void> pick(String key) async {
    if (busy) return;
    await guard(() async {
      final value = await storage.pickFolder();
      if (value == null) return;
      if (key == 'romRoot') root = value;
      if (key == 'vitaOutput') { vitaOutput = value; vitaEntries = []; existingVita = {}; }
      if (key == 'esdeRoot') esdeRoot = value;
      await settings.saveRoot(key, value);
      invalidate();
    });
  }
  Future<void> importKeys() async {
    if (busy) return;
    await guard(() async {
      final file = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'Keys', extensions: ['keys'])]);
      if (file != null) { await settings.importKeys(file.path); activity = 'prod.keys imported into private storage'; }
    });
  }
  Future<void> guard(Future<void> Function() action) async {
    error = null;
    try { await action(); } on OperationCancelled { activity = 'Stopped'; }
    catch (exception) { error = exception.toString(); }
    notifyListeners();
  }
  Future<void> scan() async {
    if (busy) return;
    final selectedTool = tool;
    final target = selectedTool == Tool.vita ? vitaOutput : selectedTool == Tool.esde ? esdeRoot : root;
    if (target == null) { error = 'Choose a folder first.'; notifyListeners(); return; }
    busy = true; activity = 'Scanning ${target.label}'; cancellation = Cancellation(); plan = null; results = [];
    notifyListeners();
    await guard(() async {
      if (selectedTool == Tool.converters) {
        final unavailable = await converters.unavailableReason(converter);
        if (unavailable != null) throw StateError(unavailable);
        if (converter == Converter.nsp && !await File(settings.keysPath).exists()) throw StateError('Import prod.keys before scanning NSZ packages.');
      }
      final entries = await storage.scan(target, cancellation!, hidden: hidden);
      OperationPlan next;
      switch (selectedTool) {
        case Tool.organiser: next = planOrganiser(entries, frontend);
        case Tool.zipper: next = planZip(entries);
        case Tool.converters: next = planConverter(entries, converter, system: system, discType: discType, deleteSource: deleteSource);
        case Tool.vita:
          vitaEntries = entries;
          await _readExistingVita(entries);
          final apps = {...{for (final a in database) a.id: a}, ...{for (final a in scannedApps) a.id: a}};
          next = planVita(apps.values.where((a) => selectedVita.contains(a.id) && !existingVita.contains(a.id)).toList(), entries, format);
        case Tool.esde:
          if (catalog == null) throw StateError('Load ES-DE definitions and select systems first.');
          next = catalog!.plan(esdeSystems, entries);
      }
      cancellation!.check();
      plan = next; planTool = selectedTool; planRoot = target;
      selected = {for (var i = 0; i < next.changes.length; i++) i};
      activity = '${next.changes.length} changes ready to review';
    });
    busy = false; notifyListeners();
  }
  Future<void> scanVitaInstalled() async {
    if (busy || root == null) return;
    busy = true; cancellation = Cancellation(); activity = 'Looking for installed Vita apps'; notifyListeners();
    await guard(() async {
      final entries = await storage.scan(root!, cancellation!, hidden: hidden);
      final apps = <String, VitaApp>{};
      for (final entry in entries.where((e) => e.name.toLowerCase() == 'param.sfo')) {
        cancellation!.check();
        final values = parseSfo(await storage.read(root!, entry.path));
        final id = values['TITLE_ID']; final title = values['TITLE'];
        if (id is String && title is String && RegExp(r'^[A-Z]{4}\d{5}$').hasMatch(id)) apps[id] = VitaApp(id, title, source: entry.path);
      }
      scannedApps = apps.values.toList(); activity = '${apps.length} installed Vita apps found';
    });
    busy = false; plan = null; selected = {}; results = []; notifyListeners();
  }
  Future<void> refreshVita() async {
    if (vitaOutput == null || busy) return;
    await guard(() async { vitaEntries = await storage.scan(vitaOutput!, Cancellation(), hidden: true); await _readExistingVita(vitaEntries); });
  }
  Future<void> _readExistingVita(List<RomEntry> entries) async {
    existingVita = {};
    for (final entry in entries.where((e) => !e.isDirectory && e.extension == format.name && e.size <= 256)) {
      final contents = String.fromCharCodes(await storage.read(vitaOutput!, entry.path));
      final match = RegExp(r'(?:\[vita_game_id\])?\s*([A-Z]{4}\d{5})\s*$').firstMatch(contents);
      if (match != null) existingVita.add(match[1]!);
    }
  }
  Future<void> removeShortcut(RomEntry entry) async {
    if (busy || vitaOutput == null || entry.extension != format.name) return;
    await guard(() async { await storage.delete(vitaOutput!, entry.path); vitaEntries.remove(entry); await _readExistingVita(vitaEntries); invalidate(); });
  }
  Future<void> loadEsDe() async {
    if (busy || root == null) return;
    busy = true; activity = 'Loading ES-DE definitions'; cancellation = Cancellation(); notifyListeners();
    await guard(() async {
      catalog = await EsDeCatalogService().fetch();
      final entries = await storage.scan(root!, cancellation!, hidden: hidden);
      final folders = entries.where((e) => e.isDirectory && !e.path.contains('/')).map((e) => e.path).toList();
      final available = {for (final s in catalog!.systems) s.name.toLowerCase(): s};
      esdeSystems = folders.where((f) => available.containsKey(f.toLowerCase())).map((f) {
        final s = available[f.toLowerCase()]!; return EsDeSystem(s.name, s.title, '%ROMPATH%/$f');
      }).toList();
      activity = '${esdeSystems.length} systems detected';
    });
    busy = false; plan = null; selected = {}; results = []; notifyListeners();
  }
  Future<void> relocate(EsDeSystem value) async {
    if (busy) return;
    await guard(() async {
      final picked = await storage.pickFolder();
      if (picked == null) return;
      final path = await storage.absolutePath(picked);
      if (path == null) throw StateError('ES-DE requires a filesystem path. Choose a folder on local storage.');
      value.path = path; invalidate();
    });
  }
  Future<void> loadInstalledEsDe() async {
    if (busy || esdeRoot == null) return;
    busy = true; activity = 'Reading installed ES-DE definitions'; notifyListeners();
    await guard(() async {
      final xml = await storage.read(esdeRoot!, 'custom_systems/es_systems.xml');
      final rules = await storage.read(esdeRoot!, 'custom_systems/es_find_rules.xml');
      catalog = EsDeCatalog(utf8.decode(xml), utf8.decode(rules));
      esdeSystems = catalog!.systems;
      plan = null; selected = {}; results = [];
    });
    busy = false; notifyListeners();
  }
  void toggle(int index, bool enabled) { enabled ? selected.add(index) : selected.remove(index); notifyListeners(); }
  void selectAll(bool enabled) { selected = enabled && plan != null ? {for (var i = 0; i < plan!.changes.length; i++) i} : {}; notifyListeners(); }
  Future<void> execute({bool overwrite = false}) async {
    if (busy || plan == null || planRoot == null) return;
    final changes = [for (final i in selected.toList()..sort()) plan!.changes[i]];
    busy = true; running = true; cancellation = Cancellation(); completed = 0; total = changes.length; fraction = 0; notifyListeners();
    await guard(() async {
      results = await PlanExecutor(storage, converters, settings.keysPath).run(planRoot!, changes, cancellation!, overwrite: overwrite,
        progress: (done, count, part, title) { completed = done; total = count; fraction = part; activity = title; notifyListeners(); });
      plan = null; selected = {}; activity = 'Batch finished';
    });
    busy = false; running = false; notifyListeners();
    if (planTool == Tool.vita) await refreshVita();
  }
  void stop(bool now) { if (now) { cancellation?.immediate = true; } else { cancellation?.afterCurrent = true; } notifyListeners(); }
}
