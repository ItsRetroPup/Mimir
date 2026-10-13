import 'models.dart';

DiscMatch? parseDisc(String fileName) {
  final stem = fileName.contains('.') ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
  final patterns = [
    RegExp(r'\s*[\(\[]?(disc|disk|cd)\s*([0-9]+)[\)\]]?', caseSensitive: false),
    RegExp(r'\s*[\(\[]?([0-9]+)\s*of\s*([0-9]+)[\)\]]?', caseSensitive: false),
  ];
  for (var i = 0; i < patterns.length; i++) {
    final match = patterns[i].firstMatch(stem);
    if (match == null) continue;
    final title = stem.replaceRange(match.start, match.end, '').trim()
      .replaceAll(RegExp(r'[\s._-]+$'), '').replaceAll(RegExp(r'\s{2,}'), ' ');
    if (title.isEmpty) return null;
    return DiscMatch(title, int.parse(match.group(i == 0 ? 2 : 1)!));
  }
  return null;
}

OperationPlan planOrganiser(List<RomEntry> entries, Frontend frontend) {
  final groups = <String, List<({RomEntry entry, DiscMatch match})>>{};
  for (final entry in entries.where((e) => !e.isDirectory)) {
    final match = parseDisc(entry.name);
    if (match != null) (groups['${entry.parent}/${match.title.toLowerCase()}'] ??= []).add((entry: entry, match: match));
  }
  final existing = entries.map((e) => e.path.toLowerCase()).toSet();
  final reserved = <String>{};
  final changes = <PlannedChange>[];
  final conflicts = <String>[];
  final buckets = groups.values.where((g) => g.length >= 2).toList()
    ..sort((a, b) => a.first.match.title.toLowerCase().compareTo(b.first.match.title.toLowerCase()));
  for (final bucket in buckets) {
    bucket.sort((a, b) => a.match.number.compareTo(b.match.number));
    final title = bucket.first.match.title;
    final parent = bucket.first.entry.parent;
    final ps2 = parent.split('/').any((s) => s.toLowerCase() == 'ps2');
    final move = ps2 || frontend == Frontend.esde;
    final folder = joinRelative(parent, ps2 ? title : '$title.m3u');
    final ops = <FileOperation>[];
    if (move) {
      ops.add(FileOperation(OperationKind.directory, folder));
      for (final item in bucket) {
        ops.add(FileOperation(OperationKind.move, joinRelative(folder, item.entry.name), source: item.entry.path));
      }
    }
    if (!ps2) {
      ops.add(FileOperation(OperationKind.text, joinRelative(move ? folder : parent, '$title.m3u'),
        contents: bucket.where((i) => i.entry.extension != 'bin').map((i) => i.entry.name).join('\n')));
    }
    if (ops.any((o) => existing.contains(o.target.toLowerCase()) || reserved.contains(o.target.toLowerCase()))) {
      conflicts.add('Skipped $title: a target already exists.');
      continue;
    }
    reserved.addAll(ops.map((o) => o.target.toLowerCase()));
    changes.add(PlannedChange(title, ops));
  }
  return OperationPlan(changes, conflicts: conflicts);
}

OperationPlan planZip(List<RomEntry> entries) {
  final existing = entries.map((e) => e.path.toLowerCase()).toSet();
  final reserved = <String>{};
  final conflicts = <String>[];
  final changes = <PlannedChange>[];
  final sources = entries.where((e) => !e.isDirectory && zipExtensions.contains(e.extension)).toList()
    ..sort((a, b) => a.path.toLowerCase().compareTo(b.path.toLowerCase()));
  for (final entry in sources) {
    final target = '${entry.stem}.zip';
    if (existing.contains(target.toLowerCase()) || !reserved.add(target.toLowerCase())) {
      conflicts.add('Skipped ${entry.name}: target already exists: $target');
      continue;
    }
    changes.add(PlannedChange(entry.name, [FileOperation(OperationKind.zip, target, source: entry.path)], sourceSize: entry.size));
  }
  return OperationPlan(changes, conflicts: conflicts);
}

OperationPlan planConverter(List<RomEntry> entries, Converter converter, {
  String system = 'Dreamcast', DiscType discType = DiscType.cd, bool deleteSource = false,
}) {
  final existing = {for (final e in entries) e.path.toLowerCase(): e.path};
  final reserved = <String>{};
  final changes = <PlannedChange>[];
  final conflicts = <String>[];
  final aliases = converter == Converter.chd ? chdSystems[system]!.aliases : converter.aliases;
  final extensions = converter == Converter.chd ? chdSystems[system]!.extensions : converter.extensions;
  final sources = entries.where((e) => !e.isDirectory && extensions.contains(e.extension) &&
    (e.parent.isEmpty || e.parent.split('/').any((s) => aliases.contains(s.toLowerCase())))).toList()
    ..sort((a, b) => a.path.toLowerCase().compareTo(b.path.toLowerCase()));
  for (final entry in sources) {
    final generated = '${entry.stem}.${converter.extension}';
    final target = existing[generated.toLowerCase()] ?? generated;
    if (!reserved.add(target.toLowerCase())) {
      conflicts.add('Skipped ${entry.name}: another image would create $target');
      continue;
    }
    changes.add(PlannedChange(entry.name, [FileOperation(OperationKind.convert, target,
      source: entry.path, converter: converter, discType: discType,
      deleteSource: converter == Converter.chd && deleteSource)],
      sourceSize: entry.size, targetExists: existing.containsKey(target.toLowerCase())));
  }
  return OperationPlan(changes, conflicts: conflicts);
}

String sanitizeVitaTitle(String title) {
  final value = title.replaceAll(RegExp(r'[<>:"/\\|?*\x00-\x1f]'), ' ')
    .replaceAll(RegExp(r'\s+'), ' ').trim().replaceAll(RegExp(r'[. ]+$'), '');
  if (value.isEmpty) return 'Unknown Vita Title';
  return RegExp(r'^(con|prn|aux|nul|com[1-9]|lpt[1-9])$', caseSensitive: false).hasMatch(value) ? '_$value' : value;
}

OperationPlan planVita(List<VitaApp> apps, List<RomEntry> entries, ShortcutFormat format) {
  final used = entries.map((e) => e.path.toLowerCase()).toSet();
  final changes = <PlannedChange>[];
  final conflicts = <String>[];
  final sorted = [...apps]..sort((a, b) {
    final title = a.title.toLowerCase().compareTo(b.title.toLowerCase());
    return title == 0 ? a.id.compareTo(b.id) : title;
  });
  for (final app in sorted) {
    final stem = sanitizeVitaTitle(app.title);
    var path = '$stem.${format.name}';
    if (used.contains(path.toLowerCase())) path = '$stem [${app.id}].${format.name}';
    if (!used.add(path.toLowerCase())) { conflicts.add('Skipped ${app.id}: shortcut already exists.'); continue; }
    changes.add(PlannedChange(app.title, [FileOperation(OperationKind.text, path,
      contents: format == ShortcutFormat.psvita ? app.id : '[vita_game_id]${app.id}')]));
  }
  return OperationPlan(changes, conflicts: conflicts);
}
