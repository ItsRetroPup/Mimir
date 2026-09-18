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
  });
}
