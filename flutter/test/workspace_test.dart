import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:mimir/app/controller.dart';
import 'package:mimir/domain/models.dart';
import 'package:mimir/main.dart';
import 'package:mimir/platform/converters.dart';
import 'package:mimir/platform/settings.dart';
import 'package:mimir/platform/storage.dart';

void main() {
  late Directory directory;
  late AppController controller;
  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    directory = await Directory.systemTemp.createTemp('mimir-ui-test-');
    controller = AppController(SettingsService(await SharedPreferences.getInstance(), directory), DesktopStorage(), BundledConverters());
  });
  tearDown(() async { await directory.delete(recursive: true); });
  Future<void> mount(WidgetTester tester, Size size, {double scale = 1}) async {
    tester.view.physicalSize = size; tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = scale;
    addTearDown(tester.view.resetPhysicalSize); addTearDown(tester.view.resetDevicePixelRatio); addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
    await tester.pumpWidget(ProviderScope(overrides: [controllerProvider.overrideWith((ref) => controller)], child: const MimirApp()));
    await tester.pumpAndSettle();
  }
  for (final size in [const Size(390, 844), const Size(844, 390), const Size(1440, 900)]) {
    testWidgets('tools render without overflow at $size', (tester) async {
      await mount(tester, size);
      for (final tool in Tool.values) {
        controller.navigate(tool); await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
      }
    });
  }
  testWidgets('compact review supports large accessibility text', (tester) async {
    controller.plan = const OperationPlan([PlannedChange('Game (Disc 1) with a long title', [FileOperation(OperationKind.text, 'psx/Game.m3u', contents: 'disc.chd')])]);
    controller.planTool = Tool.organiser; controller.planRoot = StorageRoot(directory.path, 'Library'); controller.selected = {0};
    await mount(tester, const Size(390, 844), scale: 2);
    expect(tester.takeException(), isNull);
    expect(find.text('Apply 1 changes'), findsOneWidget);
  });
  testWidgets('apply requires confirmation and cancel does not touch files', (tester) async {
    controller.plan = const OperationPlan([PlannedChange('Game', [FileOperation(OperationKind.text, 'Game.psvita', contents: 'PCSE00001')])]);
    controller.planTool = Tool.organiser; controller.planRoot = StorageRoot(directory.path, 'Library'); controller.selected = {0};
    await mount(tester, const Size(1440, 900));
    await tester.tap(find.text('Apply 1 changes')); await tester.pumpAndSettle();
    expect(find.text('Apply 1 changes?'), findsOneWidget);
    await tester.tap(find.text('Cancel')); await tester.pumpAndSettle();
    expect(await directory.list().isEmpty, isTrue);
  });
}
