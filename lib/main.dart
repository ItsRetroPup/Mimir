import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mimir_core/mimir_core.dart';

import 'l10n/app_localizations.dart';
import 'state/app_state.dart';
import 'state/ui_message.dart';

void main() {
  runApp(const ProviderScope(child: MimirApp()));
}

class MimirApp extends ConsumerWidget {
  const MimirApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final darkMode = ref.watch(
      appControllerProvider.select((state) => state.useDarkMode),
    );
    const seed = Color(0xff8b5cf6);
    final lightScheme = ColorScheme.fromSeed(
      seedColor: seed,
      brightness: Brightness.light,
    );
    final darkScheme = ColorScheme.fromSeed(
      seedColor: seed,
      brightness: Brightness.dark,
    );
    return MaterialApp(
      title: 'Mimir',
      debugShowCheckedModeBanner: false,
      theme: _theme(lightScheme),
      darkTheme: _theme(darkScheme),
      themeMode: darkMode ? ThemeMode.dark : ThemeMode.light,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: const MimirShell(),
    );
  }

  ThemeData _theme(ColorScheme scheme) => ThemeData(
    colorScheme: scheme,
    useMaterial3: true,
    visualDensity: VisualDensity.adaptivePlatformDensity,
    inputDecorationTheme: const InputDecorationTheme(
      border: OutlineInputBorder(),
    ),
    cardTheme: const CardThemeData(
      margin: EdgeInsets.zero,
      elevation: 0,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(20)),
      ),
    ),
  );
}

class MimirShell extends ConsumerWidget {
  const MimirShell({super.key});

  static const _railSections = [
    AppSection.home,
    AppSection.organizer,
    AppSection.chdMan,
    AppSection.scummVm,
    AppSection.vita,
    AppSection.esDeSystems,
  ];

  static const _compactSections = [
    AppSection.home,
    AppSection.organizer,
    AppSection.chdMan,
    AppSection.vita,
    AppSection.esDeSystems,
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);
    final wide =
        MediaQuery.sizeOf(context).width >= 840 &&
        MediaQuery.sizeOf(context).height >= 480;
    final railIndex = _railIndex(state.currentSection);
    final l10n = AppLocalizations.of(context);
    return PopScope(
      canPop: state.currentSection == AppSection.home,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop && state.currentSection != AppSection.home) {
          controller.selectSection(AppSection.home);
        }
      },
      child: Scaffold(
        appBar: AppBar(
          leading: state.currentSection == AppSection.home
              ? Padding(
                  padding: const EdgeInsets.all(9),
                  child: Image.asset('assets/mimir_launcher_m.png'),
                )
              : IconButton(
                  tooltip: l10n.goHome,
                  onPressed: () => controller.selectSection(AppSection.home),
                  icon: const Icon(Icons.arrow_back),
                ),
          title: Text(_sectionTitle(l10n, state.currentSection)),
          actions: [
            IconButton(
              tooltip: l10n.toggleDarkMode,
              onPressed: () => controller.updateDarkMode(!state.useDarkMode),
              icon: Icon(
                state.useDarkMode
                    ? Icons.light_mode_outlined
                    : Icons.dark_mode_outlined,
              ),
            ),
            const SizedBox(width: 8),
          ],
        ),
        body: Row(
          children: [
            if (wide)
              NavigationRail(
                selectedIndex: railIndex,
                onDestinationSelected: (index) =>
                    controller.selectSection(_railSections[index]),
                labelType: NavigationRailLabelType.all,
                destinations: [
                  NavigationRailDestination(
                    icon: const Icon(Icons.home_outlined),
                    selectedIcon: const Icon(Icons.home),
                    label: Text(l10n.home),
                  ),
                  NavigationRailDestination(
                    icon: const Icon(Icons.folder_copy_outlined),
                    selectedIcon: const Icon(Icons.folder_copy),
                    label: Text(l10n.toolMultidisc),
                  ),
                  NavigationRailDestination(
                    icon: const Icon(Icons.swap_horiz_outlined),
                    selectedIcon: const Icon(Icons.swap_horiz),
                    label: Text(l10n.conversionQueue),
                  ),
                  NavigationRailDestination(
                    icon: const Icon(Icons.auto_awesome_motion_outlined),
                    selectedIcon: const Icon(Icons.auto_awesome_motion),
                    label: Text(l10n.toolScummVmLaunchers),
                  ),
                  NavigationRailDestination(
                    icon: const Icon(Icons.gamepad_outlined),
                    selectedIcon: const Icon(Icons.gamepad),
                    label: Text(l10n.toolVitaShortcuts),
                  ),
                  NavigationRailDestination(
                    icon: const Icon(Icons.settings_outlined),
                    selectedIcon: const Icon(Icons.settings),
                    label: Text(l10n.toolEsdeSystems),
                  ),
                ],
              ),
            if (wide) const VerticalDivider(width: 1),
            Expanded(
              child: Align(
                alignment: Alignment.topCenter,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 1180),
                  child: _PageBody(section: state.currentSection),
                ),
              ),
            ),
          ],
        ),
        bottomNavigationBar: wide
            ? null
            : NavigationBar(
                selectedIndex: _compactIndex(state.currentSection),
                onDestinationSelected: (index) =>
                    controller.selectSection(_compactSections[index]),
                destinations: [
                  NavigationDestination(
                    icon: const Icon(Icons.home_outlined),
                    selectedIcon: const Icon(Icons.home),
                    label: l10n.home,
                  ),
                  NavigationDestination(
                    icon: Icon(Icons.folder_copy_outlined),
                    selectedIcon: Icon(Icons.folder_copy),
                    label: l10n.toolMultidisc,
                  ),
                  NavigationDestination(
                    icon: Icon(Icons.swap_horiz_outlined),
                    selectedIcon: Icon(Icons.swap_horiz),
                    label: l10n.conversionQueue,
                  ),
                  NavigationDestination(
                    icon: Icon(Icons.gamepad_outlined),
                    selectedIcon: Icon(Icons.gamepad),
                    label: l10n.toolVitaShortcuts,
                  ),
                  NavigationDestination(
                    icon: Icon(Icons.settings_outlined),
                    selectedIcon: Icon(Icons.settings),
                    label: l10n.toolEsdeSystems,
                  ),
                ],
              ),
      ),
    );
  }

  int _railIndex(AppSection section) => switch (section) {
    AppSection.home => 0,
    AppSection.organizer || AppSection.zipper => 1,
    AppSection.chdMan ||
    AppSection.rvz ||
    AppSection.zcci ||
    AppSection.nsz => 2,
    AppSection.scummVm => 3,
    AppSection.vita => 4,
    AppSection.esDeSystems => 5,
  };

  int _compactIndex(AppSection section) => switch (section) {
    AppSection.home => 0,
    AppSection.organizer || AppSection.zipper => 1,
    AppSection.chdMan ||
    AppSection.rvz ||
    AppSection.zcci ||
    AppSection.nsz => 2,
    AppSection.scummVm => 0,
    AppSection.vita => 3,
    AppSection.esDeSystems => 4,
  };

  String _sectionTitle(AppLocalizations l10n, AppSection section) =>
      switch (section) {
        AppSection.home => l10n.appName,
        AppSection.organizer => l10n.toolMultidisc,
        AppSection.zipper => l10n.toolRomzipper,
        AppSection.chdMan => l10n.toolChdman,
        AppSection.rvz => l10n.toolDolphinRvz,
        AppSection.zcci => l10n.toolAzaharZcci,
        AppSection.nsz => l10n.toolNszToNsp,
        AppSection.scummVm => l10n.toolScummVmLaunchers,
        AppSection.vita => l10n.toolVitaShortcuts,
        AppSection.esDeSystems => l10n.toolEsdeSystems,
      };
}

class _PageBody extends ConsumerWidget {
  const _PageBody({required this.section});

  final AppSection section;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(appControllerProvider);
    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 180),
      child: switch (section) {
        AppSection.home => const _HomePage(key: ValueKey('home')),
        AppSection.vita => const _VitaPage(key: ValueKey('vita')),
        AppSection.esDeSystems => const _EsDePage(key: ValueKey('esde')),
        AppSection.scummVm => const _ScummVmPage(key: ValueKey('scummvm')),
        _ => _ToolPage(key: ValueKey(section), section: section, state: state),
      },
    );
  }
}

class _HomePage extends ConsumerWidget {
  const _HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final controller = ref.read(appControllerProvider.notifier);
    final groups = [
      (
        title: l10n.homePrepareLibrary,
        tools: [
          _HomeTool(
            icon: Icons.folder_copy_outlined,
            goal: l10n.goalFixMultidisc,
            title: l10n.toolMultidisc,
            description: l10n.toolMultidiscDescription,
            onTap: () => controller.selectTool(
              section: AppSection.organizer,
              mode: ToolMode.multiDiscOrganizer,
            ),
          ),
          _HomeTool(
            icon: Icons.gamepad_outlined,
            goal: l10n.goalCreateVitaShortcuts,
            title: l10n.toolVitaShortcuts,
            description: l10n.toolVitaShortcutsDescription,
            onTap: () => controller.selectSection(AppSection.vita),
          ),
          _HomeTool(
            icon: Icons.auto_awesome_motion_outlined,
            goal: l10n.goalCreateScummVmLaunchers,
            title: l10n.toolScummVmLaunchers,
            description: l10n.toolScummVmLaunchersDescription,
            onTap: () => controller.selectSection(AppSection.scummVm),
          ),
          _HomeTool(
            icon: Icons.settings_outlined,
            goal: l10n.goalSetupEsde,
            title: l10n.toolEsdeSystems,
            description: l10n.toolEsdeSystemsDescription,
            onTap: () => controller.selectSection(AppSection.esDeSystems),
          ),
        ],
      ),
      (
        title: l10n.homeMakeRoom,
        tools: [
          _HomeTool(
            icon: Icons.inventory_2_outlined,
            goal: l10n.goalConvertSwitchPackages,
            title: l10n.toolNszToNsp,
            description: l10n.toolNszToNspDescription,
            onTap: () => controller.selectTool(
              section: AppSection.nsz,
              mode: ToolMode.chdConverter,
              converter: ConverterTool.nszNsp,
            ),
          ),
          _HomeTool(
            icon: Icons.archive_outlined,
            goal: l10n.goalCompressCartridgeRoms,
            title: l10n.toolRomzipper,
            description: l10n.toolRomzipperDescription,
            onTap: () => controller.selectTool(
              section: AppSection.zipper,
              mode: ToolMode.romZipper,
            ),
          ),
          _HomeTool(
            icon: Icons.sd_storage_outlined,
            goal: l10n.goalCompressDiscImages,
            title: l10n.toolChdman,
            description: l10n.toolChdmanDescription,
            onTap: () => controller.selectTool(
              section: AppSection.chdMan,
              mode: ToolMode.chdConverter,
              converter: ConverterTool.chd,
            ),
          ),
          _HomeTool(
            icon: Icons.gamepad_outlined,
            goal: l10n.goalShrinkGamecubeWii,
            title: l10n.toolDolphinRvz,
            description: l10n.toolDolphinRvzDescription,
            onTap: () => controller.selectTool(
              section: AppSection.rvz,
              mode: ToolMode.chdConverter,
              converter: ConverterTool.dolphinRvz,
            ),
          ),
          _HomeTool(
            icon: Icons.threed_rotation_outlined,
            goal: l10n.goalShrink3ds,
            title: l10n.toolAzaharZcci,
            description: l10n.toolAzaharZcciDescription,
            onTap: () => controller.selectTool(
              section: AppSection.zcci,
              mode: ToolMode.chdConverter,
              converter: ConverterTool.azaharZcci,
            ),
          ),
        ],
      ),
    ];
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 28, 24, 40),
      children: [
        Text(
          l10n.homeIntro,
          style: Theme.of(
            context,
          ).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 8),
        Text(l10n.homePrompt, style: Theme.of(context).textTheme.bodyLarge),
        const SizedBox(height: 28),
        for (final group in groups) ...[
          Text(
            group.title,
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 12),
          LayoutBuilder(
            builder: (context, constraints) {
              final columns = constraints.maxWidth >= 900
                  ? 3
                  : constraints.maxWidth >= 560
                  ? 2
                  : 1;
              final width =
                  (constraints.maxWidth - (columns - 1) * 14) / columns;
              return Wrap(
                spacing: 14,
                runSpacing: 14,
                children: [
                  for (final tool in group.tools)
                    SizedBox(
                      width: width,
                      child: _HomeToolCard(tool: tool),
                    ),
                ],
              );
            },
          ),
          const SizedBox(height: 28),
        ],
        Card(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.supportMimir,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 8),
                Text(l10n.supportCopy),
                const SizedBox(height: 14),
                Wrap(
                  spacing: 8,
                  children: [
                    OutlinedButton.icon(
                      onPressed: () {},
                      icon: const Icon(Icons.video_library_outlined),
                      label: Text(l10n.youtube),
                    ),
                    OutlinedButton.icon(
                      onPressed: () {},
                      icon: const Icon(Icons.coffee_outlined),
                      label: Text(l10n.kofi),
                    ),
                    OutlinedButton.icon(
                      onPressed: () {},
                      icon: const Icon(Icons.favorite_outline),
                      label: Text(l10n.buyMeACoffee),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _HomeTool {
  const _HomeTool({
    required this.icon,
    required this.goal,
    required this.title,
    required this.description,
    required this.onTap,
  });

  final IconData icon;
  final String goal;
  final String title;
  final String description;
  final VoidCallback onTap;
}

class _HomeToolCard extends StatelessWidget {
  const _HomeToolCard({required this.tool});

  final _HomeTool tool;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Card(
      child: InkWell(
        onTap: tool.onTap,
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                backgroundColor: colors.primaryContainer,
                foregroundColor: colors.onPrimaryContainer,
                child: Icon(tool.icon),
              ),
              const SizedBox(height: 18),
              Text(
                tool.goal,
                style: Theme.of(
                  context,
                ).textTheme.labelLarge?.copyWith(color: colors.primary),
              ),
              const SizedBox(height: 6),
              Text(
                tool.title,
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 6),
              Text(
                tool.description,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ToolPage extends ConsumerWidget {
  const _ToolPage({super.key, required this.section, required this.state});

  final AppSection section;
  final AppState state;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final controller = ref.read(appControllerProvider.notifier);
    final isOrganizer = state.selectedMode == ToolMode.multiDiscOrganizer;
    final isZipper = state.selectedMode == ToolMode.romZipper;
    final isChd = state.selectedMode == ToolMode.chdConverter;
    final title = isOrganizer
        ? l10n.toolMultidisc
        : isZipper
        ? l10n.toolRomzipper
        : localizedConverterLabel(l10n, state.selectedConverterTool);
    final description = isOrganizer
        ? l10n.organizerDescription
        : isZipper
        ? l10n.zipperDescription
        : localizedConverterDescription(l10n, state.selectedConverterTool);
    return ListView(
      key: ValueKey(section),
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 40),
      children: [
        _IntroCard(title: title, description: description),
        const SizedBox(height: 16),
        _FolderCard(
          title: l10n.selectedRomFolder,
          label: state.romRootHandle == null
              ? l10n.noRomFolderSelected
              : state.romRootLabel,
          buttonLabel: l10n.selectRomFolder,
          onPressed: state.busy ? null : controller.selectRomFolder,
        ),
        const SizedBox(height: 16),
        if (isOrganizer)
          _OrganizerOptions(state: state, controller: controller),
        if (isChd) _ConverterOptions(state: state, controller: controller),
        if (isZipper)
          _InfoCard(icon: Icons.archive_outlined, text: l10n.zipWhitelistInfo),
        if (isChd && state.selectedConverterTool == ConverterTool.nszNsp) ...[
          const SizedBox(height: 16),
          _NszKeysCard(state: state, controller: controller),
        ],
        const SizedBox(height: 16),
        _ProgressCard(state: state),
        if (!state.busy) ...[
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: state.romRootHandle == null ? null : controller.scan,
            icon: const Icon(Icons.search),
            label: Text(l10n.scanSelectedFolder),
          ),
        ],
        if (state.message != null) ...[
          const SizedBox(height: 12),
          _MessageBanner(message: state.message!),
        ],
        if (state.previewPlan != null) ...[
          const SizedBox(height: 18),
          _PreviewCard(state: state, controller: controller),
        ],
      ],
    );
  }
}

class _ScummVmPage extends ConsumerWidget {
  const _ScummVmPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);
    final l10n = AppLocalizations.of(context);
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 40),
      children: [
        _IntroCard(
          title: l10n.toolScummVmLaunchers,
          description: l10n.scummVmDescription,
        ),
        const SizedBox(height: 16),
        _FolderCard(
          title: l10n.scummVmGamesFolder,
          label: state.scummVmRootHandle == null
              ? l10n.noScummVmFolderSelected
              : state.scummVmRootLabel,
          buttonLabel: l10n.selectScummVmGamesFolder,
          onPressed: state.busy ? null : controller.selectScummVmRoot,
        ),
        const SizedBox(height: 16),
        Card(
          child: ListTile(
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 20,
              vertical: 8,
            ),
            leading: const Icon(Icons.apps_outlined),
            title: Text(l10n.scummVmExecutable),
            subtitle: Text(
              state.scummVmExecutableLabel.isEmpty
                  ? l10n.scummVmExecutableAuto
                  : state.scummVmExecutableLabel,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            trailing: OutlinedButton(
              onPressed: state.busy ? null : controller.selectScummVmExecutable,
              child: Text(l10n.selectScummVmExecutable),
            ),
          ),
        ),
        const SizedBox(height: 16),
        _InfoCard(icon: Icons.info_outline, text: l10n.scummVmDescription),
        const SizedBox(height: 16),
        _ProgressCard(state: state),
        if (!state.busy) ...[
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: state.scummVmRootHandle == null
                ? null
                : controller.scanScummVmGames,
            icon: const Icon(Icons.search),
            label: Text(l10n.scanSelectedFolder),
          ),
        ],
        if (state.message != null) ...[
          const SizedBox(height: 12),
          _MessageBanner(message: state.message!),
        ],
        if (state.previewPlan != null) ...[
          const SizedBox(height: 18),
          _PreviewCard(state: state, controller: controller),
        ],
      ],
    );
  }
}

class _IntroCard extends StatelessWidget {
  const _IntroCard({required this.title, required this.description});

  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Card(
      color: colors.primaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(Icons.auto_awesome_outlined, color: colors.onPrimaryContainer),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      color: colors.onPrimaryContainer,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    description,
                    style: TextStyle(color: colors.onPrimaryContainer),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FolderCard extends StatelessWidget {
  const _FolderCard({
    required this.title,
    required this.label,
    required this.buttonLabel,
    required this.onPressed,
  });

  final String title;
  final String label;
  final String buttonLabel;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) => Card(
    child: ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
      leading: const Icon(Icons.folder_outlined),
      title: Text(title),
      subtitle: Text(label, maxLines: 2, overflow: TextOverflow.ellipsis),
      trailing: OutlinedButton(onPressed: onPressed, child: Text(buttonLabel)),
    ),
  );
}

class _OrganizerOptions extends StatelessWidget {
  const _OrganizerOptions({required this.state, required this.controller});

  final AppState state;
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.frontEndTarget,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<FrontendPreset>(
              initialValue: state.selectedPreset,
              items: FrontendPreset.values
                  .map(
                    (preset) => DropdownMenuItem(
                      value: preset,
                      child: Text(
                        preset == FrontendPreset.esDe
                            ? 'ES-DE'
                            : l10n.otherFrontend,
                      ),
                    ),
                  )
                  .toList(),
              onChanged: state.busy
                  ? null
                  : (value) =>
                        value == null ? null : controller.updatePreset(value),
            ),
            CheckboxListTile(
              contentPadding: EdgeInsets.zero,
              value: state.scanHiddenFolders,
              onChanged: state.busy
                  ? null
                  : (value) =>
                        controller.updateScanHiddenFolders(value ?? false),
              title: Text(l10n.scanHiddenFolders),
              subtitle: Text(l10n.scanHiddenDescription),
              controlAffinity: ListTileControlAffinity.leading,
            ),
          ],
        ),
      ),
    );
  }
}

class _ConverterOptions extends StatelessWidget {
  const _ConverterOptions({required this.state, required this.controller});

  final AppState state;
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final isChd = state.selectedConverterTool == ConverterTool.chd;
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.conversionType,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<ConverterTool>(
              initialValue: state.selectedConverterTool,
              items: ConverterTool.values
                  .map(
                    (tool) => DropdownMenuItem(
                      value: tool,
                      child: Text(localizedConverterLabel(l10n, tool)),
                    ),
                  )
                  .toList(),
              onChanged: state.busy
                  ? null
                  : (value) => value == null
                        ? null
                        : controller.updateConverterTool(value),
            ),
            if (isChd) ...[
              const SizedBox(height: 14),
              DropdownButtonFormField<ChdSystem>(
                initialValue: state.selectedChdSystem,
                decoration: InputDecoration(labelText: l10n.system),
                items: ChdSystem.values
                    .map(
                      (system) => DropdownMenuItem(
                        value: system,
                        child: Text(system.displayName),
                      ),
                    )
                    .toList(),
                onChanged: state.busy
                    ? null
                    : (value) => value == null
                          ? null
                          : controller.updateChdSystem(value),
              ),
              if (state.selectedChdSystem == ChdSystem.playStation2) ...[
                const SizedBox(height: 14),
                DropdownButtonFormField<ChdDiscType>(
                  initialValue: state.selectedChdDiscType,
                  decoration: InputDecoration(labelText: l10n.discType),
                  items: ChdDiscType.values
                      .map(
                        (type) => DropdownMenuItem(
                          value: type,
                          child: Text(type.displayName),
                        ),
                      )
                      .toList(),
                  onChanged: state.busy
                      ? null
                      : (value) => value == null
                            ? null
                            : controller.updateChdDiscType(value),
                ),
                const SizedBox(height: 8),
                Text(l10n.ps2Guidance),
              ],
              const SizedBox(height: 8),
              CheckboxListTile(
                contentPadding: EdgeInsets.zero,
                value: state.deleteOriginalChdFiles,
                onChanged: state.busy
                    ? null
                    : (value) => controller.updateDeleteOriginalChdFiles(
                        value ?? false,
                      ),
                title: Text(l10n.deleteOriginalFiles),
                controlAffinity: ListTileControlAffinity.leading,
              ),
            ],
            if (!isChd) ...[
              const SizedBox(height: 12),
              _InfoCard(
                icon: Icons.info_outline,
                text: localizedConverterDescription(
                  l10n,
                  state.selectedConverterTool,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _NszKeysCard extends StatelessWidget {
  const _NszKeysCard({required this.state, required this.controller});

  final AppState state;
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Card(
      child: ListTile(
        leading: Icon(
          state.nszKeysConfigured ? Icons.key : Icons.key_off_outlined,
        ),
        title: Text(l10n.nszKeys),
        subtitle: Text(
          state.nszKeysConfigured
              ? l10n.nszKeysConfigured
              : l10n.nszKeysMissing,
        ),
        trailing: OutlinedButton(
          onPressed: state.busy ? null : controller.selectNszKeys,
          child: Text(
            state.nszKeysConfigured
                ? l10n.replaceProdKeys
                : l10n.importProdKeys,
          ),
        ),
      ),
    );
  }
}

class _PreviewCard extends StatelessWidget {
  const _PreviewCard({required this.state, required this.controller});

  final AppState state;
  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final plan = state.previewPlan!;
    final selected = plan.changes
        .where(
          (change) => state.selectedChangePaths.contains(change.detailPath),
        )
        .length;
    final conversion = state.selectedMode == ToolMode.chdConverter;
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 18, 20, 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    l10n.preview,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                TextButton(
                  onPressed: controller.selectAllChanges,
                  child: Text(l10n.selectAll),
                ),
                TextButton(
                  onPressed: controller.deselectAllChanges,
                  child: Text(l10n.deselectAll),
                ),
              ],
            ),
            Text(
              state.selectedMode == ToolMode.romZipper
                  ? l10n.zipPreviewSummary(plan.changes.length, selected)
                  : l10n.previewSummary(plan.changes.length, selected),
            ),
            if (plan.conflicts.isNotEmpty) ...[
              const SizedBox(height: 10),
              _InfoCard(
                icon: Icons.warning_amber_outlined,
                text: l10n.conflictsSkipped(plan.conflicts.length),
              ),
            ],
            const SizedBox(height: 14),
            if (plan.changes.isEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 20),
                child: Center(child: Text(l10n.noChanges)),
              )
            else
              ConstrainedBox(
                constraints: const BoxConstraints(maxHeight: 460),
                child: ListView.separated(
                  shrinkWrap: true,
                  itemCount: plan.changes.length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final change = plan.changes[index];
                    final checked = state.selectedChangePaths.contains(
                      change.detailPath,
                    );
                    return CheckboxListTile(
                      dense: true,
                      value: checked,
                      onChanged: (value) => controller.updateChangeSelection(
                        change.detailPath,
                        value ?? false,
                      ),
                      title: Text(
                        change.title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      subtitle: Text(
                        '${change.sourceFiles.join(', ')}\n→ ${change.targetFiles.join(', ')}',
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      secondary: change.targetAlreadyExists
                          ? Tooltip(
                              message: l10n.existingOutput,
                              child: const Icon(Icons.warning_amber_outlined),
                            )
                          : null,
                      controlAffinity: ListTileControlAffinity.leading,
                    );
                  },
                ),
              ),
            if (selected > 0) ...[
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      l10n.jobsSelected(
                        selected,
                        _formatBytes(
                          plan.changes
                              .where(
                                (change) => state.selectedChangePaths.contains(
                                  change.detailPath,
                                ),
                              )
                              .fold<int>(
                                0,
                                (sum, change) => sum + change.sourceSizeBytes,
                              ),
                        ),
                      ),
                    ),
                  ),
                  FilledButton.icon(
                    onPressed: state.busy
                        ? null
                        : () => _confirmApply(context, state, controller),
                    icon: Icon(conversion ? Icons.play_arrow : Icons.check),
                    label: Text(
                      conversion
                          ? l10n.convertSelected(selected)
                          : l10n.applySelected(selected),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Future<void> _confirmApply(
    BuildContext context,
    AppState state,
    AppController controller,
  ) async {
    final plan = state.previewPlan!;
    final hasExisting = plan.changes.any(
      (change) =>
          state.selectedChangePaths.contains(change.detailPath) &&
          change.targetAlreadyExists,
    );
    final l10n = AppLocalizations.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          hasExisting ? l10n.replaceExistingOutput : l10n.applyChanges,
        ),
        content: Text(
          hasExisting
              ? l10n.replaceOutputBody(
                  plan.changes
                      .firstWhere((change) => change.targetAlreadyExists)
                      .detailPath,
                )
              : l10n.applyConfirmationBody,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(hasExisting ? l10n.replaceOutput : l10n.applyChanges),
          ),
        ],
      ),
    );
    if (confirmed == true) await controller.applyChanges();
  }
}

class _ProgressCard extends StatelessWidget {
  const _ProgressCard({required this.state});

  final AppState state;

  @override
  Widget build(BuildContext context) {
    if (!state.busy) return const SizedBox.shrink();
    final l10n = AppLocalizations.of(context);
    final controller = ProviderScope.containerOf(
      context,
    ).read(appControllerProvider.notifier);
    final progress = state.operationProgress ?? 0;
    final progressLabel =
        state.operationProgressLabel?.resolve(l10n) ??
        state.scanProgressLabel?.resolve(l10n);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              progressLabel ?? l10n.working,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            if (state.operationProgress != null) ...[
              const SizedBox(height: 12),
              LinearProgressIndicator(value: progress),
              const SizedBox(height: 8),
              if (state.currentJobLabel != null)
                Text(
                  state.currentJobLabel!,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
            ] else ...[
              const SizedBox(height: 12),
              const LinearProgressIndicator(),
            ],
            const SizedBox(height: 14),
            Wrap(
              spacing: 8,
              children: [
                OutlinedButton(
                  onPressed: controller.stopAfterCurrent,
                  child: Text(l10n.stopAfterCurrent),
                ),
                FilledButton.tonal(
                  onPressed: controller.stopNow,
                  child: Text(l10n.stopNow),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(12),
    decoration: BoxDecoration(
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      borderRadius: const BorderRadius.all(Radius.circular(12)),
    ),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 20),
        const SizedBox(width: 10),
        Expanded(child: Text(text)),
      ],
    ),
  );
}

class _MessageBanner extends StatelessWidget {
  const _MessageBanner({required this.message});

  final UiMessage message;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Card(
      color: Theme.of(context).colorScheme.secondaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(Icons.info_outline),
            const SizedBox(width: 10),
            Expanded(child: Text(message.resolve(l10n))),
          ],
        ),
      ),
    );
  }
}

class _VitaPage extends ConsumerWidget {
  const _VitaPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);
    final l10n = AppLocalizations.of(context);
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 40),
      children: [
        _IntroCard(
          title: l10n.toolVitaShortcuts,
          description: l10n.vitaDescription,
        ),
        const SizedBox(height: 16),
        _FolderCard(
          title: l10n.shortcutOutputDirectory,
          label: state.vitaOutputHandle == null
              ? l10n.noVitaOutputSelected
              : state.vitaOutputLabel,
          buttonLabel: l10n.selectOutputFolder,
          onPressed: state.busy ? null : controller.selectVitaOutput,
        ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.shortcutFileType,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 8),
                SegmentedButton<VitaShortcutFormat>(
                  segments: [
                    for (final format in VitaShortcutFormat.values)
                      ButtonSegment(
                        value: format,
                        label: Text(format.displayName),
                      ),
                  ],
                  selected: {state.vitaShortcutFormat},
                  onSelectionChanged: state.busy
                      ? null
                      : (selection) => controller.updateVitaShortcutFormat(
                          selection.single,
                        ),
                ),
                const SizedBox(height: 8),
                Text(l10n.cocoonHint),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.shortcutDatabase,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 6),
                Text(l10n.titlesReady(state.vitaDatabaseSize)),
                const SizedBox(height: 14),
                TextField(
                  onChanged: controller.updateVitaQuery,
                  decoration: InputDecoration(
                    prefixIcon: const Icon(Icons.search),
                    hintText: l10n.searchTitleOrAppId,
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        if (state.vitaQuery.trim().isNotEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    l10n.searchResults,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 8),
                  if (state.vitaSearchResults.isEmpty)
                    Padding(
                      padding: EdgeInsets.all(16),
                      child: Text(l10n.noMatchingTitles),
                    ),
                  for (final app in state.vitaSearchResults)
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.gamepad_outlined),
                      title: Text(app.title),
                      subtitle: Text(app.titleId),
                      trailing:
                          state.addedVitaShortcuts.containsKey(app.titleId)
                          ? IconButton(
                              tooltip: l10n.deleteShortcut(app.title),
                              onPressed: state.busy
                                  ? null
                                  : () => controller.removeVitaShortcut(app),
                              icon: const Icon(Icons.delete_outline),
                            )
                          : FilledButton.tonal(
                              onPressed: state.busy
                                  ? null
                                  : () => controller.addVitaShortcut(app),
                              child: Text(l10n.quickAdd),
                            ),
                    ),
                ],
              ),
            ),
          ),
        if (state.message != null) ...[
          const SizedBox(height: 12),
          _MessageBanner(message: state.message!),
        ],
      ],
    );
  }
}

class _EsDePage extends ConsumerWidget {
  const _EsDePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);
    final l10n = AppLocalizations.of(context);
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 40),
      children: [
        _IntroCard(
          title: l10n.toolEsdeSystems,
          description: l10n.esdeFolderDescription,
        ),
        const SizedBox(height: 16),
        _FolderCard(
          title: l10n.selectedEsdeFolder,
          label: state.esdeRootHandle == null
              ? l10n.noEsdeFolderSelected
              : state.esdeRootLabel,
          buttonLabel: l10n.selectEsdeFolder,
          onPressed: state.busy ? null : controller.selectEsdeRoot,
        ),
        const SizedBox(height: 16),
        _FolderCard(
          title: l10n.romRootFolder,
          label: state.romRootHandle == null
              ? l10n.noRomFolderSelected
              : state.romRootLabel,
          buttonLabel: l10n.selectRomRoot,
          onPressed: state.busy ? null : controller.selectRomFolder,
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: state.busy ? null : controller.refreshEsDeSystems,
                icon: const Icon(Icons.download_outlined),
                label: Text(l10n.downloadLatestXmls),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: FilledButton.icon(
                onPressed: state.busy || state.esdeSystems.isEmpty
                    ? null
                    : controller.applyEsDeSystems,
                icon: const Icon(Icons.install_desktop_outlined),
                label: Text(l10n.installCustomSystems),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        if (state.esdeSystems.isEmpty)
          _InfoCard(icon: Icons.info_outline, text: l10n.esdeEmpty)
        else
          Card(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    l10n.systemsCount(state.esdeSystems.length),
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 8),
                  for (final system in state.esdeSystems)
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.folder_outlined),
                      title: Text(system.fullName),
                      subtitle: Text(
                        system.romFolder,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      trailing: system.isDefault
                          ? Chip(label: Text(l10n.defaultLabel))
                          : TextButton(
                              onPressed: state.busy
                                  ? null
                                  : () => controller.pickEsDeSystemFolder(
                                      system.name,
                                    ),
                              child: Text(l10n.chooseFolder),
                            ),
                    ),
                ],
              ),
            ),
          ),
        if (state.message != null) ...[
          const SizedBox(height: 12),
          _MessageBanner(message: state.message!),
        ],
      ],
    );
  }
}

String _formatBytes(int bytes) {
  if (bytes < 1024) return '$bytes B';
  const units = ['KB', 'MB', 'GB', 'TB'];
  var value = bytes.toDouble();
  var unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  return '${value.toStringAsFixed(value >= 10 ? 0 : 1)} ${units[unit]}';
}
