import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../domain/models.dart';
import '../domain/planners.dart';
import '../platform/storage.dart';
import 'controller.dart';
import 'localization.dart';

const toolTitles = ['ROM Organiser', 'RomZipper', 'Converter Tools', 'Vita Shortcuts', 'ES-DE Systems'];
const toolDescriptions = [
  'Group multi-disc games and create playlists for your frontend.',
  'Compress supported ROMs into ZIP archives. Source files are kept.',
  'Convert disc images and packages using tools bundled with Mimir.',
  'Find Vita titles and create shortcuts for your frontend.',
  'Prepare platform-specific ES-DE definitions and relocate system folders.',
];
const toolIcons = [Icons.folder_copy_outlined, Icons.folder_zip_outlined, Icons.transform, Icons.sports_esports_outlined, Icons.settings_input_component_outlined];
const toolKeys = ['tool_multidisc', 'tool_romzipper', 'goal_compress_disc_images', 'tool_vita_shortcuts', 'tool_esde_systems'];
String toolTitle(BuildContext context, Tool tool) => MimirStrings.of(context, toolKeys[tool.index], toolTitles[tool.index]);

class Workspace extends ConsumerWidget {
  const Workspace({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final app = ref.watch(controllerProvider);
    final scheme = Theme.of(context).colorScheme;
    return LayoutBuilder(builder: (context, constraints) {
      final expanded = constraints.maxWidth >= 840;
      return Scaffold(
        appBar: AppBar(title: const Text('Mimir'), actions: [
          IconButton(tooltip: app.dark ? 'Use light theme' : 'Use dark theme', onPressed: () => app.setDark(!app.dark), icon: Icon(app.dark ? Icons.light_mode_outlined : Icons.dark_mode_outlined)),
          IconButton(tooltip: 'About and licenses', onPressed: () => showAboutDialog(context: context, applicationName: 'Mimir', applicationVersion: '0.4.0',
            children: [const Text('An open-source, local-first emulation toolkit by RetroPup. No account required.')]), icon: const Icon(Icons.info_outline)),
        ]),
        drawer: expanded ? null : Drawer(child: SafeArea(child: ListView(children: [
          const Padding(padding: EdgeInsets.all(24), child: Text('Mimir tools', style: TextStyle(fontSize: 24))),
          for (final tool in Tool.values) ListTile(selected: app.tool == tool, leading: Icon(toolIcons[tool.index]), title: Text(toolTitle(context, tool)),
            onTap: () { app.navigate(tool); Navigator.pop(context); }),
        ]))),
        body: SafeArea(child: Row(children: [
          if (expanded) NavigationRail(extended: constraints.maxWidth >= 1200, selectedIndex: app.tool.index,
            onDestinationSelected: (i) => app.navigate(Tool.values[i]),
            destinations: [for (final tool in Tool.values) NavigationRailDestination(icon: Icon(toolIcons[tool.index]), label: Text(toolTitle(context, tool)))]),
          if (expanded) VerticalDivider(width: 1, color: scheme.outlineVariant),
          Expanded(child: Column(children: [
            Expanded(child: ListView(padding: EdgeInsets.all(expanded ? 32 : 20), children: [
              Text(toolTitle(context, app.tool), style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 8), Text(toolDescriptions[app.tool.index], style: Theme.of(context).textTheme.bodyLarge),
              const SizedBox(height: 28),
              if (app.error != null) Padding(padding: const EdgeInsets.only(bottom: 20), child: Material(color: scheme.errorContainer, borderRadius: BorderRadius.circular(12),
                child: Padding(padding: const EdgeInsets.all(16), child: SelectableText(app.error!, style: TextStyle(color: scheme.onErrorContainer))))),
              _Folder(label: app.tool == Tool.vita ? 'Vita output folder' : app.tool == Tool.esde ? 'ES-DE configuration folder' : 'ROM folder',
                root: app.tool == Tool.vita ? app.vitaOutput : app.tool == Tool.esde ? app.esdeRoot : app.root,
                onPick: app.busy ? null : () => app.pick(app.tool == Tool.vita ? 'vitaOutput' : app.tool == Tool.esde ? 'esdeRoot' : 'romRoot')),
              const SizedBox(height: 20),
              if (app.tool == Tool.organiser) _OrganiserOptions(app: app),
              if (app.tool == Tool.converters) _ConverterOptions(app: app),
              if (app.tool == Tool.vita) _VitaTools(app: app),
              if (app.tool == Tool.esde) _EsDeTools(app: app),
              if (app.tool != Tool.vita) CheckboxListTile(contentPadding: EdgeInsets.zero, value: app.hidden,
                onChanged: app.busy ? null : (value) => app.setHidden(value!), title: const Text('Include hidden folders'), controlAffinity: ListTileControlAffinity.leading),
              const SizedBox(height: 20),
              if (app.plan != null && app.planTool == app.tool) _Review(app: app, wide: constraints.maxWidth >= 1000),
              if (app.results.isNotEmpty && app.planTool == app.tool) _Results(app: app),
              if (app.plan == null && app.results.isEmpty && !app.busy && app.tool != Tool.vita && app.tool != Tool.esde)
                Padding(padding: const EdgeInsets.symmetric(vertical: 32), child: Text('Choose a folder and scan to preview changes. Your files stay untouched until you confirm.', style: TextStyle(color: scheme.onSurfaceVariant))),
            ])),
            _ActionBar(app: app),
          ])),
        ])),
      );
    });
  }
}

class _Folder extends StatelessWidget {
  const _Folder({required this.label, required this.root, required this.onPick});
  final String label;
  final StorageRoot? root;
  final VoidCallback? onPick;
  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Text(label, style: Theme.of(context).textTheme.titleMedium), const SizedBox(height: 8),
    Wrap(spacing: 16, runSpacing: 8, crossAxisAlignment: WrapCrossAlignment.center, children: [
      OutlinedButton.icon(onPressed: onPick, icon: const Icon(Icons.folder_open), label: Text(root == null ? 'Choose folder' : 'Change folder')),
      if (root != null) ConstrainedBox(constraints: const BoxConstraints(maxWidth: 600), child: SelectableText(root!.label)),
    ]),
  ]);
}

class _OrganiserOptions extends StatelessWidget {
  const _OrganiserOptions({required this.app});
  final AppController app;
  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Text('Frontend preset', style: Theme.of(context).textTheme.titleMedium), const SizedBox(height: 8),
    SegmentedButton<Frontend>(segments: const [ButtonSegment(value: Frontend.esde, label: Text('ES-DE')), ButtonSegment(value: Frontend.other, label: Text('Other'))],
      selected: {app.frontend}, onSelectionChanged: app.busy ? null : (values) => app.configure(() => app.frontend = values.first)),
    const SizedBox(height: 12), Text(app.frontend == Frontend.esde ? 'Groups discs inside a .m3u folder with a playlist. PS2 uses a game folder without a playlist.' : 'Creates a playlist beside the discs. PS2 uses a game folder without a playlist.'),
  ]);
}

class _ConverterOptions extends StatelessWidget {
  const _ConverterOptions({required this.app});
  final AppController app;
  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Wrap(spacing: 8, runSpacing: 8, children: [for (final converter in Converter.values)
      ChoiceChip(label: Text(converter.label), selected: converter == app.converter,
        onSelected: app.busy ? null : (_) => app.configure(() => app.converter = converter))]),
    const SizedBox(height: 16),
    if (app.converter == Converter.chd) ...[
      DropdownButtonFormField<String>(initialValue: app.system, decoration: const InputDecoration(labelText: 'System'),
        items: [for (final system in chdSystems.keys) DropdownMenuItem(value: system, child: Text(system))],
        onChanged: app.busy ? null : (value) => app.configure(() => app.system = value!)),
      const SizedBox(height: 16),
      SegmentedButton<DiscType>(segments: const [ButtonSegment(value: DiscType.cd, label: Text('CD')), ButtonSegment(value: DiscType.dvd, label: Text('DVD'))], selected: {app.discType},
        onSelectionChanged: app.busy ? null : (values) => app.configure(() => app.discType = values.first)),
      const SizedBox(height: 12), const Text('PSP: DVD. PS2: CD for NetherSX2, DVD for ARMSX2.'),
      CheckboxListTile(contentPadding: EdgeInsets.zero, value: app.deleteSource, onChanged: app.busy ? null : (v) => app.setDeleteSource(v!),
        title: const Text('Delete sources after verified CHD conversion'), subtitle: const Text('Includes same-folder tracks referenced by CUE/GDI descriptors.'), controlAffinity: ListTileControlAffinity.leading),
    ],
    if (app.converter == Converter.rvz) const Text('ISO to RVZ · 128 KiB blocks · Zstandard level 5'),
    if (app.converter == Converter.zcci) const Text('Decrypted .3ds / .cci to .zcci'),
    if (app.converter == Converter.nsp) ...[
      const Text('NSZ to NSP requires your own prod.keys. Imported keys remain in private app storage.'), const SizedBox(height: 12),
      OutlinedButton.icon(onPressed: app.busy ? null : app.importKeys, icon: const Icon(Icons.key), label: const Text('Import prod.keys')),
    ],
  ]);
}

class _Review extends StatelessWidget {
  const _Review({required this.app, required this.wide});
  final AppController app;
  final bool wide;
  @override
  Widget build(BuildContext context) {
    final changes = app.plan!.changes;
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text('Review changes', style: Theme.of(context).textTheme.titleLarge), const SizedBox(height: 8),
      if (changes.isEmpty) const Text('No compatible changes found. Check the folder and options.'),
      if (changes.isNotEmpty) CheckboxListTile(contentPadding: EdgeInsets.zero, controlAffinity: ListTileControlAffinity.leading,
        title: Text('Select all (${changes.length})'), value: app.selected.length == changes.length,
        onChanged: app.busy ? null : (v) => app.selectAll(v!)),
      if (wide && changes.isNotEmpty) const Row(children: [SizedBox(width: 56), Expanded(child: Text('Source')), Expanded(child: Text('Output'))]),
      for (var i = 0; i < changes.length; i++) ...[
        const Divider(),
        if (wide) Row(children: [
          Checkbox(value: app.selected.contains(i), onChanged: app.busy ? null : (v) => app.toggle(i, v!)),
          Expanded(child: Padding(padding: const EdgeInsets.all(8), child: Text(changes[i].title))),
          Expanded(child: Padding(padding: const EdgeInsets.all(8), child: Text('${changes[i].targets.join('\n')}${changes[i].targetExists ? '\nExisting output — overwrite confirmation required' : ''}'))),
        ]) else CheckboxListTile(contentPadding: EdgeInsets.zero, controlAffinity: ListTileControlAffinity.leading,
          value: app.selected.contains(i), onChanged: app.busy ? null : (v) => app.toggle(i, v!),
          title: Text(changes[i].title), subtitle: Text('${changes[i].targets.join('\n')}${changes[i].targetExists ? '\nExisting output' : ''}')),
      ],
      if (app.plan!.conflicts.isNotEmpty) ...[const SizedBox(height: 20), Text('Skipped conflicts', style: Theme.of(context).textTheme.titleMedium),
        for (final conflict in app.plan!.conflicts) Padding(padding: const EdgeInsets.only(top: 8), child: Text(conflict))],
    ]);
  }
}

class _Results extends StatelessWidget {
  const _Results({required this.app});
  final AppController app;
  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Text('Results', style: Theme.of(context).textTheme.titleLarge), const SizedBox(height: 12),
    Text(ItemStatus.values.map((status) => '${app.results.where((r) => r.status == status).length} ${status.name}').join(' · ')),
    const SizedBox(height: 12),
    for (final result in app.results) ListTile(contentPadding: EdgeInsets.zero,
      leading: Icon(switch (result.status) { ItemStatus.completed => Icons.check_circle_outline, ItemStatus.failed => Icons.error_outline, ItemStatus.skipped => Icons.skip_next_outlined, ItemStatus.cancelled => Icons.stop_circle_outlined }),
      title: Text(result.title), subtitle: Text(result.message.isEmpty ? result.status.name : result.message)),
  ]);
}

class _ActionBar extends StatelessWidget {
  const _ActionBar({required this.app});
  final AppController app;
  Future<void> confirm(BuildContext context) async {
    final selected = app.selected.map((i) => app.plan!.changes[i]).toList();
    final overwrite = selected.any((c) => c.targetExists);
    final deletes = selected.any((c) => c.operations.any((o) => o.deleteSource));
    final accepted = await showDialog<bool>(context: context, builder: (context) => AlertDialog(
      title: Text('Apply ${selected.length} changes?'),
      content: Text('Mimir will apply the selected changes in ${app.planRoot!.label}.${overwrite ? '\n\nExisting output files will be replaced.' : ''}${deletes ? '\n\nSources and referenced tracks will be deleted after successful CHD verification. This cannot be undone.' : ''}'),
      actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
        FilledButton(onPressed: () => Navigator.pop(context, true), child: Text(overwrite ? 'Overwrite and apply' : 'Apply changes'))]));
    if (accepted == true) await app.execute(overwrite: overwrite);
  }
  @override
  Widget build(BuildContext context) => Material(color: Theme.of(context).colorScheme.surfaceContainer, child: Padding(padding: const EdgeInsets.all(16), child:
    app.busy ? Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(app.running ? '${app.completed} of ${app.total} · ${app.activity}' : app.activity, maxLines: 2, overflow: TextOverflow.ellipsis),
      const SizedBox(height: 8), LinearProgressIndicator(value: app.running && app.total > 0 ? (app.completed + app.fraction) / app.total : null),
      const SizedBox(height: 8), Wrap(spacing: 12, runSpacing: 8, children: [
        OutlinedButton(onPressed: () => app.stop(true), child: const Text('Stop now')),
        if (app.running) TextButton(onPressed: () => app.stop(false), child: const Text('Finish current and stop')),
      ]),
    ]) : Wrap(spacing: 12, runSpacing: 8, crossAxisAlignment: WrapCrossAlignment.center, children: [
      OutlinedButton.icon(onPressed: app.scan, icon: const Icon(Icons.search), label: Text(app.tool == Tool.esde || app.tool == Tool.vita ? 'Preview changes' : 'Scan folder')),
      FilledButton.icon(onPressed: app.plan != null && app.planTool == app.tool && app.selected.isNotEmpty ? () => confirm(context) : null,
        icon: const Icon(Icons.check), label: Text('Apply ${app.planTool == app.tool ? app.selected.length : 0} changes')),
    ])));
}

class _VitaTools extends StatefulWidget {
  const _VitaTools({required this.app});
  final AppController app;
  @override
  State<_VitaTools> createState() => _VitaToolsState();
}
class _VitaToolsState extends State<_VitaTools> {
  String query = '';
  @override
  Widget build(BuildContext context) {
    final app = widget.app;
    final matches = {...{for (final a in app.database) a.id: a}, ...{for (final a in app.scannedApps) a.id: a}}.values
      .where((a) => query.isNotEmpty && ('${a.title} ${a.id}').toLowerCase().contains(query.toLowerCase())).take(100).toList();
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      SegmentedButton<ShortcutFormat>(segments: const [ButtonSegment(value: ShortcutFormat.psvita, label: Text('.psvita')), ButtonSegment(value: ShortcutFormat.dpt, label: Text('.dpt'))],
        selected: {app.format}, onSelectionChanged: app.busy ? null : (v) => app.setFormat(v.first)),
      const SizedBox(height: 20), TextField(decoration: const InputDecoration(labelText: 'Search title or app ID', prefixIcon: Icon(Icons.search)),
        onChanged: (value) => setState(() => query = value)),
      const SizedBox(height: 12), Text('${app.selectedVita.length} titles selected. Showing up to 100 search matches.'),
      for (final value in matches) CheckboxListTile(contentPadding: EdgeInsets.zero, title: Text(value.title), subtitle: Text('${value.id}${app.existingVita.contains(value.id) ? ' · Shortcut exists' : ''}'), value: app.selectedVita.contains(value.id),
        onChanged: app.busy || app.existingVita.contains(value.id) ? null : (v) { v! ? app.selectedVita.add(value.id) : app.selectedVita.remove(value.id); app.invalidate(); }),
      const SizedBox(height: 20), _Folder(label: 'Installed Vita apps folder (optional)', root: app.root, onPick: app.busy ? null : () => app.pick('romRoot')),
      const SizedBox(height: 12), OutlinedButton(onPressed: app.busy || app.root == null ? null : app.scanVitaInstalled, child: const Text('Scan installed apps')),
      for (final value in app.scannedApps) CheckboxListTile(contentPadding: EdgeInsets.zero, title: Text(value.title), subtitle: Text(value.id), value: app.selectedVita.contains(value.id),
        onChanged: app.busy ? null : (v) { v! ? app.selectedVita.add(value.id) : app.selectedVita.remove(value.id); app.invalidate(); }),
      const SizedBox(height: 20), OutlinedButton(onPressed: app.busy || app.vitaOutput == null ? null : app.refreshVita, child: const Text('Refresh existing shortcuts')),
      for (final entry in app.vitaEntries.where((e) => e.extension == app.format.name)) ListTile(contentPadding: EdgeInsets.zero, title: Text(entry.name),
        trailing: IconButton(tooltip: 'Remove shortcut', icon: const Icon(Icons.delete_outline), onPressed: app.busy ? null : () async {
          final accepted = await showDialog<bool>(context: context, builder: (context) => AlertDialog(title: const Text('Remove shortcut?'), content: Text(entry.path),
            actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')), FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Remove'))]));
          if (accepted == true) await app.removeShortcut(entry);
        })),
    ]);
  }
}

class _EsDeTools extends StatelessWidget {
  const _EsDeTools({required this.app});
  final AppController app;
  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    _Folder(label: 'ROM root for system detection', root: app.root, onPick: app.busy ? null : () => app.pick('romRoot')),
    const SizedBox(height: 12), OutlinedButton(onPressed: app.busy || app.root == null ? null : app.loadEsDe, child: const Text('Load definitions and detect systems')),
    const SizedBox(height: 12), OutlinedButton(onPressed: app.busy || app.esdeRoot == null ? null : app.loadInstalledEsDe, child: const Text('Edit installed definitions')),
    const SizedBox(height: 12), const Text('Definitions are downloaded from ES-DE and, on Android, GlazedBelmont’s custom systems. Review paths before installing. Restart ES-DE after installation.'),
    for (final system in app.esdeSystems) ListTile(contentPadding: EdgeInsets.zero, title: Text(system.title), subtitle: Text(system.path),
      trailing: Wrap(children: [IconButton(tooltip: 'Relocate system folder', onPressed: app.busy ? null : () => app.relocate(system), icon: const Icon(Icons.folder_open)),
        IconButton(tooltip: 'Use default ROM path', onPressed: app.busy ? null : () { system.path = '%ROMPATH%/${system.name}'; app.invalidate(); }, icon: const Icon(Icons.restore))])),
  ]);
}
