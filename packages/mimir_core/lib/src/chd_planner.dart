import 'models.dart';

class ChdPlanner {
  ChdPlanner._();

  static OperationPlan buildPlan({
    required List<RomEntry> entries,
    required ChdSystem system,
    required ChdDiscType discType,
    required bool deleteOriginalFiles,
  }) {
    final changes = <PlannedChange>[];
    final operations = <FileOperation>[];
    final conflicts = <String>[];
    final existingPaths = <String, RomEntry>{
      for (final entry in entries) entry.relativePath.toLowerCase(): entry,
    };
    final reservedPaths = <String>{};
    final sorted =
        entries
            .where(
              (entry) => system.supportedExtensions.contains(
                _extension(entry.fileName),
              ),
            )
            .toList()
          ..sort(
            (a, b) => a.relativePath.toLowerCase().compareTo(
              b.relativePath.toLowerCase(),
            ),
          );
    for (final entry in sorted) {
      final dot = entry.relativePath.lastIndexOf('.');
      final stem = dot < 0
          ? entry.relativePath
          : entry.relativePath.substring(0, dot);
      final generatedTargetPath = '$stem.chd';
      final targetPath =
          existingPaths[generatedTargetPath.toLowerCase()]?.relativePath ??
          generatedTargetPath;
      if (!reservedPaths.add(targetPath)) {
        conflicts.add(
          'Skipped ${entry.fileName}: another selected image would create $targetPath',
        );
        continue;
      }
      final operation = ConvertToChd(
        sourcePath: entry.resolvedSourcePath,
        targetPath: targetPath,
        system: system,
        discType: discType,
        deleteOriginalFiles: deleteOriginalFiles,
      );
      operations.add(operation);
      changes.add(
        PlannedChange(
          title: entry.fileName,
          sourceFiles: [entry.relativePath],
          targetFiles: [targetPath],
          detailLabel: '${discType.displayName} CHD',
          detailPath: targetPath,
          operations: [operation],
          sourceSizeBytes: entry.sizeBytes,
          targetAlreadyExists: existingPaths.containsKey(
            targetPath.toLowerCase(),
          ),
        ),
      );
    }
    return OperationPlan(
      mode: ToolMode.chdConverter,
      changes: changes,
      operations: operations,
      conflicts: conflicts.toSet().toList(),
    );
  }

  static String _extension(String name) {
    final dot = name.lastIndexOf('.');
    return dot < 0 ? '' : name.substring(dot + 1).toLowerCase();
  }
}
