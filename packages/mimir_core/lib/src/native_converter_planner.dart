import 'models.dart';

class NativeConverterPlanner {
  NativeConverterPlanner._();

  static OperationPlan buildPlan({
    required List<RomEntry> entries,
    required ConverterTool tool,
  }) {
    if (tool == ConverterTool.chd) {
      throw ArgumentError('CHD conversions use ChdPlanner.');
    }
    final existingPaths = <String, RomEntry>{
      for (final entry in entries) entry.relativePath.toLowerCase(): entry,
    };
    final reservedPaths = <String>{};
    final conflicts = <String>[];
    final changes = <PlannedChange>[];
    final sorted =
        entries
            .where(
              (entry) =>
                  tool.sourceExtensions.contains(_extension(entry.fileName)),
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
      final generatedTarget = '$stem.${tool.outputExtension}';
      final targetPath =
          existingPaths[generatedTarget.toLowerCase()]?.relativePath ??
          generatedTarget;
      if (!reservedPaths.add(targetPath.toLowerCase())) {
        conflicts.add(
          'Skipped ${entry.fileName}: another selected image would create $targetPath',
        );
        continue;
      }
      final operation = ConvertWithTool(
        sourcePath: entry.resolvedSourcePath,
        targetPath: targetPath,
        tool: tool,
      );
      changes.add(
        PlannedChange(
          title: entry.fileName,
          sourceFiles: [entry.relativePath],
          targetFiles: [targetPath],
          detailLabel: tool.displayName,
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
      operations: changes.expand((change) => change.operations).toList(),
      conflicts: conflicts.toSet().toList(),
    );
  }

  static String _extension(String name) {
    final dot = name.lastIndexOf('.');
    return dot < 0 ? '' : name.substring(dot + 1).toLowerCase();
  }
}
