import 'models.dart';

class RomZipperPlanner {
  RomZipperPlanner._();

  static const supportedExtensions = <String>{
    'gb',
    'gbc',
    'gba',
    'nds',
    'fds',
    'fig',
    'nes',
    'sfc',
    'smc',
    'swc',
    'n64',
    'v64',
    'z64',
    'a26',
    'a52',
    'a78',
    'lnx',
    '32x',
    'gen',
    'md',
    'smd',
    'gg',
    'sc',
    'sg',
    'sms',
    'ngc',
    'ngp',
    'pce',
    'sgx',
    'ws',
    'wsc',
    'col',
    'cv',
    'd64',
    'mx1',
    'mx2',
    'rom',
    'sna',
    'tap',
    'tzx',
    'z80',
  };

  static OperationPlan buildPlan(List<RomEntry> entries) {
    final changes = <PlannedChange>[];
    final operations = <FileOperation>[];
    final conflicts = <String>[];
    final existingPaths = entries.map((entry) => entry.relativePath).toSet();
    final reservedPaths = <String>{};

    final sorted =
        entries
            .where(
              (entry) =>
                  _extension(entry.fileName) != null &&
                  supportedExtensions.contains(_extension(entry.fileName)),
            )
            .toList()
          ..sort(
            (a, b) => a.relativePath.toLowerCase().compareTo(
              b.relativePath.toLowerCase(),
            ),
          );
    for (final entry in sorted) {
      final dot = entry.relativePath.lastIndexOf('.');
      final stemPath = dot < 0
          ? entry.relativePath
          : entry.relativePath.substring(0, dot);
      final targetPath = '$stemPath.zip';
      if (existingPaths.contains(targetPath) ||
          !reservedPaths.add(targetPath)) {
        conflicts.add(
          'Skipped ${entry.fileName}: target already exists: $targetPath',
        );
        continue;
      }
      final operation = ZipFile(
        sourcePath: entry.resolvedSourcePath,
        targetPath: targetPath,
        archiveEntryName: entry.fileName,
      );
      operations.add(operation);
      changes.add(
        PlannedChange(
          title: entry.fileName,
          sourceFiles: [entry.relativePath],
          targetFiles: [targetPath],
          detailLabel: 'Zip',
          detailPath: targetPath,
          operations: [operation],
        ),
      );
    }
    return OperationPlan(
      mode: ToolMode.romZipper,
      changes: changes,
      operations: operations,
      conflicts: conflicts.toSet().toList(),
    );
  }

  static String? _extension(String name) {
    final dot = name.lastIndexOf('.');
    return dot < 0 ? null : name.substring(dot + 1).toLowerCase();
  }
}
