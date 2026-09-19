import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mimir_flutter/main.dart';

void main() {
  testWidgets('Mimir shows the home tools', (WidgetTester tester) async {
    await tester.pumpWidget(const ProviderScope(child: MimirApp()));
    await tester.pumpAndSettle();

    expect(find.text('Welcome to Mimir!'), findsOneWidget);
    expect(find.text('Multi-disc Organizer'), findsWidgets);
    expect(find.text('Vita Shortcuts'), findsWidgets);
    expect(find.text('ScummVM Launchers'), findsWidgets);
  });

  testWidgets('opening a home tool shows its ready-to-configure screen', (
    WidgetTester tester,
  ) async {
    tester.view.physicalSize = const Size(1200, 1200);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(const ProviderScope(child: MimirApp()));
    await tester.pumpAndSettle();

    await tester.tap(
      find.ancestor(of: find.text('RomZipper'), matching: find.byType(InkWell)),
    );
    await tester.pumpAndSettle();

    expect(find.text('Selected ROM folder'), findsOneWidget);
    expect(find.text('Select ROM folder'), findsOneWidget);
    expect(find.text('Scan selected folder'), findsOneWidget);
  });
}
