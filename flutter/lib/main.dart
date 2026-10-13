import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'app/controller.dart';
import 'app/workspace.dart';
import 'app/localization.dart';
import 'platform/settings.dart';
import 'platform/storage.dart';
import 'platform/converters.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    final settings = await SettingsService.load();
    final controller = AppController(settings, Platform.isAndroid ? AndroidStorage() : DesktopStorage(), BundledConverters());
    await controller.loadDatabase();
    await MimirStrings.load();
    for (final license in ['NSZ-MIT.txt', 'MAME-GPL-2.0.txt', 'Dolphin-GPL-2.0.txt', 'Azahar-GPL-2.0.txt']) {
      final text = await rootBundle.loadString('assets/licenses/$license');
      LicenseRegistry.addLicense(() => Stream.value(LicenseEntryWithLineBreaks([license.split('-').first], text)));
    }
    runApp(ProviderScope(overrides: [controllerProvider.overrideWith((ref) => controller)], child: const MimirApp()));
  } catch (error) {
    runApp(MaterialApp(home: Scaffold(body: Center(child: Padding(padding: const EdgeInsets.all(24),
      child: SelectableText('Mimir could not start.\n$error\nRestart the app or install a complete release.'))))));
  }
}

class MimirApp extends ConsumerWidget {
  const MimirApp({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final app = ref.watch(controllerProvider);
    ThemeData theme(Brightness brightness) {
      final scheme = ColorScheme.fromSeed(seedColor: const Color(0xff7b1fa2), brightness: brightness);
      return ThemeData(useMaterial3: true, colorScheme: scheme, scaffoldBackgroundColor: scheme.surface,
        visualDensity: VisualDensity.standard,
        inputDecorationTheme: const InputDecorationTheme(border: OutlineInputBorder(), contentPadding: EdgeInsets.all(16)),
        filledButtonTheme: FilledButtonThemeData(style: FilledButton.styleFrom(minimumSize: const Size(48, 48))),
        outlinedButtonTheme: OutlinedButtonThemeData(style: OutlinedButton.styleFrom(minimumSize: const Size(48, 48))));
    }
    return MaterialApp(title: 'Mimir', debugShowCheckedModeBanner: false, theme: theme(Brightness.light),
      darkTheme: theme(Brightness.dark), themeMode: app.dark ? ThemeMode.dark : ThemeMode.light,
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      supportedLocales: const [Locale('en'), Locale('de'), Locale('es'), Locale('fr'), Locale('it'), Locale('ja'), Locale('ko'), Locale('pl'), Locale('pt', 'BR'), Locale('zh', 'CN'), Locale('zh', 'TW')],
      home: const Workspace());
  }
}
