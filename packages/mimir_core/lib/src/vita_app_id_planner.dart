import 'models.dart';

class VitaAppIdPlanner {
  VitaAppIdPlanner._();

  static OperationPlan buildPlan({
    required List<VitaApp> apps,
    List<RomEntry> existingEntries = const [],
    VitaShortcutFormat format = VitaShortcutFormat.psvita,
  }) {
    final operations = <FileOperation>[];
    final changes = <PlannedChange>[];
    final conflicts = <String>[];
    final existingPaths = existingEntries
        .map((entry) => entry.relativePath)
        .toSet();
    final reservedPaths = <String>{};
    final sorted = [...apps]
      ..sort((a, b) {
        final title = a.title.toLowerCase().compareTo(b.title.toLowerCase());
        return title == 0 ? a.titleId.compareTo(b.titleId) : title;
      });
    for (final app in sorted) {
      final preferredFileName =
          '${_sanitizeFileStem(app.title)}.${format.extension}';
      final fallbackFileName =
          '${_sanitizeFileStem(app.title)} [${app.titleId}].${format.extension}';
      final targetPath =
          _canReserve(preferredFileName, existingPaths, reservedPaths)
          ? preferredFileName
          : _canReserve(fallbackFileName, existingPaths, reservedPaths)
          ? fallbackFileName
          : null;
      if (targetPath == null) {
        conflicts.add(
          'Skipped ${app.titleId}: target already exists for ${app.title}',
        );
        continue;
      }
      final operation = WriteTextFile(
        relativePath: targetPath,
        contents: _fileContents(app.titleId, format),
      );
      operations.add(operation);
      changes.add(
        PlannedChange(
          title: app.title,
          sourceFiles: [
            app.sourcePath == 'shortcut-db'
                ? 'Database match (${app.titleId})'
                : '${app.sourcePath} (${app.titleId})',
          ],
          targetFiles: [targetPath],
          detailLabel: 'Create',
          detailPath: targetPath,
          operations: [operation],
        ),
      );
    }
    return OperationPlan(
      mode: ToolMode.vitaAppIds,
      changes: changes,
      operations: operations,
      conflicts: conflicts.toSet().toList(),
    );
  }

  static String _fileContents(String titleId, VitaShortcutFormat format) =>
      switch (format) {
        VitaShortcutFormat.psvita => titleId,
        VitaShortcutFormat.dpt => '[vita_game_id]$titleId',
      };

  static bool _canReserve(
    String path,
    Set<String> existingPaths,
    Set<String> reservedPaths,
  ) {
    if (existingPaths.contains(path)) return false;
    return reservedPaths.add(path);
  }

  static String _sanitizeFileStem(String title) {
    final cleaned = title.split('').map((char) {
      return '<>:"/\\|?*'.contains(char) ? ' ' : char;
    }).join();
    final normalized = cleaned.replaceAll(RegExp(r'\s+'), ' ').trim();
    final withoutDots = normalized.replaceFirst(RegExp(r'\.+$'), '');
    return withoutDots.isEmpty ? 'Unknown Vita Title' : withoutDots;
  }
}
